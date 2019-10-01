import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PeerTable {
    private static final long TIMEOUT_MILLIS = 10000;
    private HashMap<InetAddress, Long> peerTable = new HashMap<>();

    public void refresh(InetAddress peerAddress) {
        peerTable.put(peerAddress, System.currentTimeMillis());
    }

    public void doCleanup() {
        Iterator<Map.Entry<InetAddress, Long>> it = peerTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<InetAddress, Long> peerTableEntry = it.next();
            long lastTimeSeen = peerTableEntry.getValue();
            if (System.currentTimeMillis() - lastTimeSeen >= TIMEOUT_MILLIS) {
                it.remove();
            }
        }
    }

    public void print() {
        clearScreen();

        System.out.println("Peers list:");
        System.out.println("Address\t\t\tLast time seen");
        for (Map.Entry<InetAddress, Long> peerTableEntry: peerTable.entrySet()) {
            InetAddress peerAddress = peerTableEntry.getKey();
            Date lastTimeSeen = new Date(peerTableEntry.getValue());
            System.out.println(String.format("%s\t\t%s", peerAddress.toString(), lastTimeSeen.toString()));
        }
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
