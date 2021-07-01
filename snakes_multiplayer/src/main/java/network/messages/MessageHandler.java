package network.messages;

import network.messages.auxiliary.AckMessageID;
import network.messages.auxiliary.HandledMessage;
import network.messages.auxiliary.ReceivedMessage;
import network.suppliers.AckMessageIdSupplier;
import network.suppliers.HandledMessageSupplier;
import network.suppliers.ReceivedMessageSupplier;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class MessageHandler implements Runnable, AckMessageIdSupplier, HandledMessageSupplier {
    private BlockingQueue<HandledMessage> handledMessageQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<AckMessageID> ackMessageIdQueue = new LinkedBlockingQueue<>();

    private ReceivedMessageSupplier receivedMessageSupplier;

    public MessageHandler(ReceivedMessageSupplier receivedMessageSupplier) {
        this.receivedMessageSupplier = receivedMessageSupplier;
    }

    @Override
    public AckMessageID getNextAckMessageID() throws InterruptedException {
        return ackMessageIdQueue.take();
    }

    @Override
    public HandledMessage getNextHandledMessage() throws InterruptedException {
        return handledMessageQueue.take();
    }

    @Override
    public final void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ReceivedMessage receivedMessage = receivedMessageSupplier.getNextReceivedMessage();
                HandledMessage handledMessage = handleMessage(receivedMessage);
                if (handledMessage == null) {
                    continue;
                }

                handledMessageQueue.add(handledMessage);
                if (handledMessage.isAckMessage()) {
                    AckMessageID ackMessageID = handledMessage.getAckMessageID();
                    ackMessageIdQueue.add(ackMessageID);
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

    public abstract HandledMessage handleMessage(ReceivedMessage receivedMessage) throws Exception;
}
