package chatnode;

import chatnode.messages.Message;

import java.net.InetSocketAddress;
import java.util.*;

public class SendSession implements Runnable {
    public static final int ATTEMPTS_NUMBER = 3;
    public static final int TIMEOUT_MILLIS = 2000;

    private ChatNode chatNode;
    private final Map<UUID, Message> messages = new HashMap<>();
    private volatile boolean hasFinished = false;

    public SendSession(List<Message> messages, ChatNode chatNode) {
        this.chatNode = chatNode;
        for (Message message: messages) {
            this.messages.put(message.getUUID(), message);
        }
    }

    @Override
    public void run() {
        try {
            for (int attemptsRemained = ATTEMPTS_NUMBER;
                 attemptsRemained > 0 && !Thread.currentThread().isInterrupted();
                --attemptsRemained) {

                for (Message message: messages.values()) {
                    chatNode.getMessageChannel().sendMessage(message);
                }

                Thread.sleep(TIMEOUT_MILLIS);

                messages.keySet().removeIf(messageUUID ->
                    chatNode.getMessageSender().messageWasDelivered(messageUUID)
                );
                if (messages.isEmpty()) {
                    return;
                }
            }

            detachInactiveNeighbours();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("Send session interruption error: %s\n", e.getMessage());
        } catch (Exception e) {
            System.err.printf("Send session error: %s\n", e.getMessage());
        } finally {
            synchronized (this) {
                hasFinished = true;
                notifyAll();
            }
        }

    }

    public synchronized void awaitTermination() throws InterruptedException {
        while (!hasFinished) {
            wait();
        }
    }

    private void detachInactiveNeighbours() {
        TreeStructureInfo treeStructureInfo = chatNode.getTreeStructureInfo();
        synchronized (treeStructureInfo) {
            Set<InetSocketAddress> neighbours = treeStructureInfo.getNeighbours();

            for (Message message: messages.values()) {
                InetSocketAddress neighbourAddress = message.getRemoteSocketAddress();
                neighbours.remove(neighbourAddress);

                if (neighbourAddress.equals(treeStructureInfo.getParentAddress())) {
                    InetSocketAddress newParentAddress = treeStructureInfo.getParentReserveNodeAddress();
                    chatNode.setParent(newParentAddress);
                    chatNode.getMessageSender().sendConnectMessage();
                    if (newParentAddress == null && !neighbours.isEmpty()) {
                        treeStructureInfo.setReserveNodeAddress(neighbours.iterator().next());
                    }
                    chatNode.getMessageSender().broadcastReserveNodeMessage();
                } else if (neighbourAddress.equals(treeStructureInfo.getReserveNodeAddress())) {
                    if (neighbours.isEmpty()) {
                        treeStructureInfo.setReserveNodeAddress(null);
                    } else {
                        treeStructureInfo.setReserveNodeAddress(neighbours.iterator().next());
                        chatNode.getMessageSender().broadcastReserveNodeMessage();
                    }
                }
            }

        }
    }
}
