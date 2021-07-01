package chatnode;

import chatnode.messages.Message;
import chatnode.messages.MessageHandler;
import chatnode.messages.handlers.*;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageReceiver implements Runnable {
    private ChatNode chatNode;
    private final Set<UUID> handledMessages = new HashSet<>();
    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    public MessageReceiver(ChatNode chatNode) {
        this.chatNode = chatNode;
        singleThreadExecutor.submit(this);
    }

    public void shutdown() {
        singleThreadExecutor.shutdownNow();
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Message message = chatNode.getMessageChannel().receiveMessage();
                InetSocketAddress remoteAddress = message.getRemoteSocketAddress();

                if (message.getType() != Message.CONNECT_TYPE) {
                    TreeStructureInfo treeStructureInfo = chatNode.getTreeStructureInfo();
                    synchronized (treeStructureInfo) {
                        boolean senderIsNeighbour = treeStructureInfo.getNeighbours().contains(remoteAddress);
                        if (!senderIsNeighbour) {
                            continue;
                        }
                    }
                }

                if (message.getType() != Message.ACK_TYPE) {
                    chatNode.getMessageSender().sendAckMessage(remoteAddress, message.getUUID());
                    boolean messageWasHandled = !handledMessages.add(message.getUUID());
                    if (messageWasHandled) {
                        continue;
                    }
                }

                MessageHandler messageHandler;
                switch (message.getType()) {
                    case Message.ACK_TYPE:
                        messageHandler = new AckMessageHandler(chatNode);
                        break;
                    case Message.CONNECT_TYPE:
                        messageHandler = new ConnectMessageHandler(chatNode);
                        break;
                    case Message.RESERVE_NODE_TYPE:
                        messageHandler = new ReserveNodeMessageHandler(chatNode);
                        break;
                    case Message.TEXT_TYPE:
                        messageHandler = new TextMessageHandler(chatNode);
                        break;
                    case Message.HEART_BEAT_TYPE:
                        messageHandler = new HeartBeatMessageHandler(chatNode);
                        break;
                    default:
                        continue;
                }

                messageHandler.handleMessage(message);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.printf("Message receiver error: %s\n", e.getMessage());
        }
    }
}
