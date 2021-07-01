package network.integrity;

import network.messages.auxiliary.PingMessage;
import network.messages.auxiliary.SentMessage;
import network.messages.delivery.MessageDeliveryService;
import network.suppliers.SentMessageSupplier;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NodePinger implements Runnable {
    private MessageDeliveryService messageDeliveryService;
    private SentMessageSupplier sentMessageSupplier;
    private PingMessageEncoder pingMessageEncoder;

    private ExecutorService sentMessagesListener = Executors.newSingleThreadExecutor();

    private long pingDelayMillis;
    private Map<Integer, SenderActivity> senderActivityMap = new HashMap<>();
    private SortedSet<SenderActivity> senderActivitySet = new TreeSet<>();

    private static class SenderActivity implements Comparable<SenderActivity> {
        private int receiverID;
        private long lastSentMessageTime;

        SenderActivity(int receiverID, long lastSentMessageTime) {
            this.receiverID = receiverID;
            this.lastSentMessageTime = lastSentMessageTime;
        }

        @Override
        public int compareTo(SenderActivity o) {
            return Long.compare(lastSentMessageTime, o.lastSentMessageTime);
        }

        int getReceiverID() {
            return receiverID;
        }

        long getInactivityTime() {
            return System.currentTimeMillis() - lastSentMessageTime;
        }

        long getLastSentMessageTime() {
            return lastSentMessageTime;
        }

        void setLastSentMessageTime(long lastSentMessageTime) {
            this.lastSentMessageTime = lastSentMessageTime;
        }
    }


    public NodePinger(long pingDelayMillis,
                      MessageDeliveryService messageDeliveryService,
                      SentMessageSupplier sentMessageSupplier,
                      PingMessageEncoder pingMessageEncoder) {
        this.pingDelayMillis = pingDelayMillis;
        this.messageDeliveryService = messageDeliveryService;
        this.sentMessageSupplier = sentMessageSupplier;
        this.pingMessageEncoder = pingMessageEncoder;
    }

    public synchronized void addReceiverID(int receiverID, long lastSentMessageTime) {
        if (senderActivityMap.containsKey(receiverID)) {
            SenderActivity senderActivity = senderActivityMap.get(receiverID);
            if (senderActivity.getLastSentMessageTime() < lastSentMessageTime) {
                senderActivitySet.remove(senderActivity);
                senderActivity.setLastSentMessageTime(lastSentMessageTime);
                senderActivitySet.add(senderActivity);
            }
            return;
        }

        SenderActivity senderActivity = new SenderActivity(receiverID, lastSentMessageTime);
        senderActivityMap.put(receiverID, senderActivity);
        senderActivitySet.add(senderActivity);

        notify();
    }

    public synchronized void removeReceiverID(int receiverID) {
        SenderActivity senderActivity = senderActivityMap.remove(receiverID);
        if (senderActivity != null) {
            senderActivitySet.remove(senderActivity);
        }
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

                sendPingMessages();
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

    private long calculateTimeToSleep() {
        if (senderActivitySet.isEmpty()) {
            return 0;
        }

        SenderActivity senderActivity = senderActivitySet.first();
        long inactivityTime = senderActivity.getInactivityTime();
        long timeRemained = pingDelayMillis - inactivityTime;

        return timeRemained == 0 ? -1 : timeRemained;
    }

    private void sendPingMessages() throws IOException {
        Iterator<SenderActivity> setIterator = senderActivitySet.iterator();
        while (setIterator.hasNext()) {
            SenderActivity senderActivity = setIterator.next();
            long inactivityTime = senderActivity.getInactivityTime();
            if (inactivityTime < pingDelayMillis) {
                break;
            }

            setIterator.remove();
            int receiverID = senderActivity.getReceiverID();

            PingMessage pingMessage = pingMessageEncoder.getPingMessage(receiverID);
            if (pingMessage != null) {
                messageDeliveryService.sendMessageWithAck(
                        pingMessage.getMessage(),
                        pingMessage.getReceiverSocketAddress()
                );
            }
        }
    }

    private void startAuxiliaryThreads() {
        sentMessagesListener.submit(this::listenSentMessages);
    }

    private void listenSentMessages() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                SentMessage sentMessage = sentMessageSupplier.getNextSentMessage();
                Integer receiverID = sentMessage.getMessage().getReceiverID();
                if (receiverID == null) {
                    continue;
                }

                synchronized (this) {
                    SenderActivity senderActivity;
                    Long lastSentMessageTime = sentMessage.getTimestamp();
                    if (!senderActivityMap.containsKey(receiverID)) {
                        continue;
                    }

                    senderActivity = senderActivityMap.get(receiverID);
                    senderActivitySet.remove(senderActivity);
                    senderActivity.setLastSentMessageTime(lastSentMessageTime);
                    senderActivitySet.add(senderActivity);

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
        sentMessagesListener.shutdownNow();
    }
}
