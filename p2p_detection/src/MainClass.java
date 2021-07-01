import java.io.IOException;

public class MainClass {
    private static final int CLI_ARGS_NUMBER = 1;
    private static final int FAILURE_EXIT_CODE = -1;

    public static void main(String[] args) {
        if (args.length != CLI_ARGS_NUMBER) {
            printUsage();
            System.exit(FAILURE_EXIT_CODE);
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

        System.exit(FAILURE_EXIT_CODE);
    }

    private static void printUsage() {
        System.out.println("Usage: java <MainClass path> <local multicast group address (IPv4 or IPv6)>");
    }
}
