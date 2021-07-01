package chatnode;

import chatnode.messages.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageSender {
    private ChatNode chatNode;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private Set<UUID> deliveredMessages = ConcurrentHashMap.newKeySet();

    public MessageSender(ChatNode chatNode) {
        this.chatNode = chatNode;
    }

    public void shutdown() {
        threadPool.shutdownNow();
    }

    public void addDeliveredMessage(UUID messageUUID) {
        deliveredMessages.add(messageUUID);
    }

    public boolean messageWasDelivered(UUID messageUUID) {
        return deliveredMessages.contains(messageUUID);
    }

    public void sendAckMessage(InetSocketAddress remoteAddress, UUID messageUUID) throws IOException {
        Message message = new Message(messageUUID, Message.ACK_TYPE, null, remoteAddress);
        chatNode.getMessageChannel().sendMessage(message);
    }

    public SendSession sendConnectMessage() {
        InetSocketAddress parentAddress = chatNode.getTreeStructureInfo().getParentAddress();
        if (parentAddress == null) {
            return null;
        }

        Message message = new Message(UUID.randomUUID(), Message.CONNECT_TYPE, null, parentAddress);
        return createSendSession(message);
    }

    public SendSession sendReserveNodeMessage(InetSocketAddress remoteAddress) {
        byte[] messageBody = formReserveNodeMessageBody();
        Message message = new Message(UUID.randomUUID(), Message.RESERVE_NODE_TYPE, messageBody, remoteAddress);
        return createSendSession(message);
    }

    public SendSession broadcastReserveNodeMessage() {
        List<Message> messages;
        TreeStructureInfo treeStructureInfo = chatNode.getTreeStructureInfo();
        synchronized (treeStructureInfo) {
            Set<InetSocketAddress> neighbours = treeStructureInfo.getNeighbours();
            if (neighbours.isEmpty()) {
                return null;
            }

            messages = new ArrayList<>();
            InetSocketAddress parentAddress = treeStructureInfo.getParentAddress();
            byte[] messageBody = formReserveNodeMessageBody();
            for (InetSocketAddress neighbour: neighbours) {
                if (neighbour.equals(parentAddress)) {
                    continue;
                }
                Message message = new Message(UUID.randomUUID(), Message.RESERVE_NODE_TYPE, messageBody, neighbour);
                messages.add(message);
            }
        }

        return createSendSession(messages);
    }

    private byte[] formReserveNodeMessageBody() {
        InetSocketAddress reserveNodeSocketAddress = chatNode.getTreeStructureInfo().getReserveNodeAddress();

        int reservePort = reserveNodeSocketAddress.getPort();
        byte[] reserveAddressBytes = reserveNodeSocketAddress.getAddress().getAddress();

        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES + reserveAddressBytes.length);
        byteBuffer.put(reserveAddressBytes);
        byteBuffer.putInt(reservePort);

        return byteBuffer.array();
    }

    public SendSession sendHeartBeatMessage() {
        InetSocketAddress reserveNodeAddress = chatNode.getTreeStructureInfo().getReserveNodeAddress();
        if (reserveNodeAddress == null) {
            return null;
        }

        Message message = new Message(UUID.randomUUID(), Message.HEART_BEAT_TYPE, null, reserveNodeAddress);
        return createSendSession(message);
    }

    public SendSession sendTextMessage(String nodeName, String textMessage) {
        byte[] nodeNameBytes = nodeName.getBytes(StandardCharsets.UTF_8);
        byte[] textMessageBytes = textMessage.getBytes(StandardCharsets.UTF_8);

        ByteBuffer byteBuffer = ByteBuffer.allocate(2 * Integer.BYTES + nodeNameBytes.length + textMessageBytes.length);
        byteBuffer.putInt(nodeNameBytes.length);
        byteBuffer.putInt(textMessageBytes.length);
        byteBuffer.put(nodeNameBytes);
        byteBuffer.put(textMessageBytes);
        byte[] messageBodyBytes = byteBuffer.array();

        InetSocketAddress ownAddress = chatNode.getMessageChannel().getSocketAddress();
        return sendTextMessage(ownAddress, messageBodyBytes);
    }

    public SendSession sendTextMessage(InetSocketAddress senderAddress, byte[] messageBodyBytes) {
        List<Message> messages;
        TreeStructureInfo treeStructureInfo = chatNode.getTreeStructureInfo();
        synchronized (treeStructureInfo) {
            Set<InetSocketAddress> neighbours = treeStructureInfo.getNeighbours();
            if (neighbours.isEmpty()) {
                return null;
            }

            messages = new ArrayList<>();
            for (InetSocketAddress neighbourAddress: neighbours) {
                if (neighbourAddress.equals(senderAddress)) {
                    continue;
                }

                Message message = new Message(UUID.randomUUID(), Message.TEXT_TYPE, messageBodyBytes, neighbourAddress);
                messages.add(message);
            }
        }

        return createSendSession(messages);
    }

    private SendSession createSendSession(Message message) {
        return createSendSession(Collections.singletonList(message));
    }

    private SendSession createSendSession(List<Message> messages) {
        SendSession sendSession = new SendSession(messages, chatNode);
        threadPool.submit(sendSession);
        return sendSession;
    }
}
