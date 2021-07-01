package network.messages;

import network.messages.auxiliary.ReceivedMessage;
import network.suppliers.ReceivedMessageSupplier;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.concurrent.*;

public class MessageReceiver implements Runnable, ReceivedMessageSupplier {
    private static final int MAX_PACKET_SIZE = 65535;

    private DatagramSocket socket;
    private MulticastSocket multicastSocket;

    private DatagramPacket packet;
    private DatagramPacket multicastPacket;

    private ExecutorService multicastSocketThread = Executors.newSingleThreadExecutor();
    private BlockingQueue<ReceivedMessage> messageQueue = new LinkedBlockingQueue<>();

    public MessageReceiver(DatagramSocket socket,
                           MulticastSocket multicastSocket) {
        this.socket = socket;
        packet = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);

        this.multicastSocket = multicastSocket;
        multicastPacket = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
    }

    @Override
    public ReceivedMessage getNextReceivedMessage() throws InterruptedException {
        return messageQueue.take();
    }

    @Override
    public void run() {
        try {
            startMulticastThread();
            receiveMessages(socket, packet);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            stopMulticastThread();
        }
    }

    private void startMulticastThread() {
        multicastSocketThread.submit(() -> {
            try {
                receiveMessages(multicastSocket, multicastPacket);
            } catch (RuntimeException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        });
    }

    private void stopMulticastThread() {
        multicastSocketThread.shutdownNow();
    }

    private void receiveMessages(DatagramSocket socket, DatagramPacket packet)
                                                            throws IOException, InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            socket.receive(packet);

            long timestamp = System.currentTimeMillis();
            byte[] messageBytes = Arrays.copyOf(packet.getData(), packet.getLength());
            InetSocketAddress senderSocketAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());

            ReceivedMessage message = new ReceivedMessage();
            message.setSenderSocketAddress(senderSocketAddress);
            message.setMessageBytes(messageBytes);
            message.setTimestamp(timestamp);

            messageQueue.put(message);
        }
    }
}
