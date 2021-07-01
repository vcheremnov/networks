package network.messages;

import network.messages.auxiliary.Message;
import network.messages.auxiliary.SentMessage;
import network.messages.delivery.MessageDeliveryService;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageSender implements MessageDeliveryService {
    private InetSocketAddress groupSocketAddress;

    private DatagramSocket socket;
    private BlockingQueue<SentMessage> sentMessagesWithAckQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<SentMessage> sentMessagesQueue = new LinkedBlockingQueue<>();

    public MessageSender(DatagramSocket socket, InetSocketAddress groupSocketAddress) {
        this.socket = socket;
        this.groupSocketAddress = groupSocketAddress;
    }

    @Override
    public SentMessage getNextSentMessageWithAck() throws InterruptedException {
        return sentMessagesWithAckQueue.take();
    }

    @Override
    public SentMessage getNextSentMessage() throws InterruptedException {
        return sentMessagesQueue.take();
    }

    @Override
    public SentMessage sendMessage(Message message, InetSocketAddress socketAddress) throws IOException {
        sendMessageBytes(message.getMessageBytes(), socketAddress);

        SentMessage sentMessage = new SentMessage();
        sentMessage.setTimestamp(System.currentTimeMillis());
        sentMessage.setReceiverSocketAddress(socketAddress);
        sentMessage.setMessage(message);

        sentMessagesQueue.add(sentMessage);

        return sentMessage;
    }

    @Override
    public SentMessage sendMessageWithAck(Message message, InetSocketAddress socketAddress) throws IOException {
        SentMessage sentMessage = sendMessage(message, socketAddress);
        sentMessagesWithAckQueue.add(sentMessage);
        return sentMessage;
    }

    @Override
    public SentMessage sendMulticastMessage(Message message) throws IOException {
        return sendMessage(message, groupSocketAddress);
    }

    private void sendMessageBytes(byte[] messageBytes, SocketAddress socketAddress) throws IOException {
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, socketAddress);
        socket.send(packet);
    }
}
