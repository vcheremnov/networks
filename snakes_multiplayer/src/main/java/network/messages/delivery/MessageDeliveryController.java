package network.messages.delivery;

import network.messages.auxiliary.AckMessageID;
import network.messages.auxiliary.SentMessage;
import network.suppliers.AckMessageIdSupplier;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageDeliveryController implements Runnable {
    private final long ackTimeoutMillis;
    private final Set<Integer> receiverIdSet = new HashSet<>();
    private final SortedSet<SentMessage> sentMessages = new TreeSet<>();
    private final Map<AckMessageID, SentMessage> sentMessagesMap = new HashMap<>();

    private ExecutorService ackMessageListener = Executors.newSingleThreadExecutor();
    private ExecutorService sentMessageListener = Executors.newSingleThreadExecutor();

    private AckMessageIdSupplier ackMessageIdSupplier;
    private MessageDeliveryService messageDeliveryService;

    public MessageDeliveryController(long ackTimeoutMillis,
                                     AckMessageIdSupplier ackMessageIdSupplier,
                                     MessageDeliveryService messageDeliveryService) {
        this.ackTimeoutMillis = ackTimeoutMillis;
        this.ackMessageIdSupplier = ackMessageIdSupplier;
        this.messageDeliveryService = messageDeliveryService;
    }

    public synchronized void addReceiverID(int nodeID) {
        receiverIdSet.add(nodeID);
    }

    public synchronized void removeReceiverID(int nodeID) {
        receiverIdSet.remove(nodeID);
    }

    @Override
    public synchronized void run() {
        try {
            startAuxiliaryThreads();
            while (!Thread.currentThread().isInterrupted()) {
                long timeToSleep = calculateTimeToSleep();
                while (timeToSleep >= 0) {
                    wait(timeToSleep);
                    timeToSleep = calculateTimeToSleep();
                }

                resendMessages();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            stopAuxiliaryThreads();
        }
    }

    private void startAuxiliaryThreads() {
        ackMessageListener.submit(this::listenAckMessages);
        sentMessageListener.submit(this::listenSentMessages);
    }

    private void listenAckMessages() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                AckMessageID ackMessageID = ackMessageIdSupplier.getNextAckMessageID();
                synchronized (this) {
                    SentMessage sentMessage = sentMessagesMap.remove(ackMessageID);
                    if (sentMessage != null) {
                        sentMessages.remove(sentMessage);
                    }
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void listenSentMessages() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                SentMessage sentMessage = messageDeliveryService.getNextSentMessageWithAck();
                synchronized (this) {
                    AckMessageID ackMessageID = sentMessage.getAckMessageID();
                    sentMessagesMap.put(ackMessageID, sentMessage);
                    sentMessages.add(sentMessage);
                    notify();
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void stopAuxiliaryThreads() {
        ackMessageListener.shutdownNow();
        sentMessageListener.shutdownNow();
    }

    private long calculateTimeToSleep() {
        if (sentMessages.isEmpty()) {
            return 0;
        }

        SentMessage sentMessage = sentMessages.first();
        long timeSinceSent = sentMessage.getTimeSinceSent();
        long timeRemained = ackTimeoutMillis - timeSinceSent;

        return timeRemained == 0 ? -1 : timeRemained;
    }

    private void resendMessages() throws IOException {
        Iterator<SentMessage> setIterator = sentMessages.iterator();
        while (setIterator.hasNext()) {
            SentMessage sentMessage = setIterator.next();
            long timeSinceSent = sentMessage.getTimeSinceSent();
            if (timeSinceSent < ackTimeoutMillis) {
                break;
            }

            AckMessageID ackMessageID = sentMessage.getAckMessageID();
            sentMessagesMap.remove(ackMessageID);
            setIterator.remove();

            if (receiverIdSet.contains(ackMessageID.getNodeID())) {
                messageDeliveryService.sendMessageWithAck(
                        sentMessage.getMessage(),
                        sentMessage.getReceiverSocketAddress()
                );
            }
        }
    }
}
