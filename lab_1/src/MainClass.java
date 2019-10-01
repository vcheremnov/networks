import java.io.IOException;

public class MainClass {
    public static void main(String[] args) {
        if (args.length != 1) {
            printUsage();
            System.exit(-1);
        }

        try {
            PeerDetector peerDetector = new PeerDetector(args[0]);
            peerDetector.run();
            return;
        } catch (PeerDetectorAddressException e) {
            System.err.println(e.getMessage());
            printUsage();
        } catch (IOException | PeerDetectorRunException e) {
            System.err.println(e.getMessage());
        }

        System.exit(-1);
    }

    private static void printUsage() {
        System.out.println("Usage: java <MainClass path> <local multicast group address (IPv4 or IPv6)>");
    }
}
