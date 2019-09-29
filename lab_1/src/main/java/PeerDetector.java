import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class PeerDetector {
    private static final int PROTOCOL_PORT = 12345;
    private static final long SEND_DELAY_MILLIS = 1000;
    private static final long TIMEOUT_MILLIS = 10000;

    private MulticastSocket socket;
    private InetAddress groupAddress;
    private HashMap<InetAddress, Long> peerTable;

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

            peerTable = new HashMap<>();
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
            reprintTable();
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
                    refreshTable(senderAddress);
                    reprintTable();
                }

                doTableCleanup();
            }
        } finally {
            socket.leaveGroup(groupAddress);
            socket.close();
        }
    }

    private void refreshTable(InetAddress peerAddress) {
        peerTable.put(peerAddress, System.currentTimeMillis());
    }

    private void reprintTable() {
        clearScreen();

        System.out.println("Peers list:");
        System.out.println("Address\t\t\tLast time seen");
        for (Entry<InetAddress, Long> peerTableEntry: peerTable.entrySet()) {
            InetAddress peerAddress = peerTableEntry.getKey();
            Date lastTimeSeen = new Date(peerTableEntry.getValue());
            System.out.println(String.format("%s\t\t%s", peerAddress.toString(), lastTimeSeen.toString()));
        }
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void doTableCleanup() {
        Iterator<Entry<InetAddress, Long>> it = peerTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<InetAddress, Long> peerTableEntry = it.next();
            long lastTimeSeen = peerTableEntry.getValue();
            if (System.currentTimeMillis() - lastTimeSeen >= TIMEOUT_MILLIS) {
                it.remove();
            }
        }
    }
}
