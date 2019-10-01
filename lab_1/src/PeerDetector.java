import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class PeerDetector {
    private static final int PROTOCOL_PORT = 12345;
    private static final long SEND_DELAY_MILLIS = 1000;

    private MulticastSocket socket;
    private InetAddress groupAddress;
    private PeerTable peerTable = new PeerTable();

    PeerDetector(String groupAddressString) throws PeerDetectorAddressException, IOException {
        try {
            groupAddress = InetAddress.getByName(groupAddressString);

            if (!groupAddress.isMulticastAddress()) {
                throw new PeerDetectorAddressException(
                        String.format("Address %s is not a multicast address", groupAddressString)
                );
            }

            if (!groupAddress.isMCLinkLocal()) {
                throw new PeerDetectorAddressException(
                    String.format("Address %s is not a local multicast address", groupAddressString)
                );
            }

            socket = new MulticastSocket(PROTOCOL_PORT);
            socket.joinGroup(groupAddress);
        } catch (UnknownHostException e) {
            throw new PeerDetectorAddressException(
                String.format("Address %s is invalid", groupAddressString), e
            );
        }
    }

    public void run() throws PeerDetectorRunException, IOException {
        if (socket.isClosed()) {
            throw new PeerDetectorRunException("Peer detector has been already executed");
        }

        try {
            peerTable.print();
            DatagramPacket sendPacket = new DatagramPacket(new byte[0], 0, 0, groupAddress, PROTOCOL_PORT);
            DatagramPacket recvPacket = new DatagramPacket(new byte[0], 0);

            for (;;) {
                socket.send(sendPacket);
                long lastSendTime = System.currentTimeMillis();

                for (long timeSinceSend = 0;
                     timeSinceSend < SEND_DELAY_MILLIS;
                     timeSinceSend = System.currentTimeMillis() - lastSendTime) {

                    try {
                        int timeLeft = (int) (SEND_DELAY_MILLIS - timeSinceSend);
                        socket.setSoTimeout(timeLeft);
                        socket.receive(recvPacket);
                    } catch (SocketTimeoutException ex) {
                        break;
                    }

                    InetAddress senderAddress = recvPacket.getAddress();
                    peerTable.refresh(senderAddress);
                    peerTable.print();
                }

                peerTable.doCleanup();
            }
        } finally {
            socket.leaveGroup(groupAddress);
            socket.close();
        }
    }
}
