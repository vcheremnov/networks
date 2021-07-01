package chatnode.messages;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class MessageChannel {
    private static final int MAX_PACKET_SIZE = 1024;
    private DatagramPacket recvPacket = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);

    private InetSocketAddress socketAddress;
    private DatagramSocket socket;
    private int lossPercentage;

    public MessageChannel(String hostname, int port, int lossPercentage) throws SocketException {
        socket = new DatagramSocket(new InetSocketAddress(hostname, port));
        this.socketAddress = new InetSocketAddress(socket.getLocalAddress(), port);
        this.lossPercentage = lossPercentage;
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    public Message receiveMessage() throws IOException {
        Message message = null;
        while (message == null) {
            socket.receive(recvPacket);

            int randomInt = ThreadLocalRandom.current().nextInt(0, 100);
            if (randomInt < lossPercentage) {
                // the message has been lost
                continue;
            }

            byte[] messageBytes = Arrays.copyOf(recvPacket.getData(), recvPacket.getLength());
            InetSocketAddress socketAddress = new InetSocketAddress(recvPacket.getAddress(), recvPacket.getPort());
            message = new Message(messageBytes, socketAddress);
        }

        return message;
    }

    public void sendMessage(Message message) throws IOException {
        byte[] data = message.getMessageBytes();
        DatagramPacket packet = new DatagramPacket(data, 0, data.length, message.getRemoteSocketAddress());
        socket.send(packet);
    }

    public void close() {
        socket.close();
    }
}
