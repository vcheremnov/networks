import proxy.ProxyServer;

public class Main {
    private static final int SERVER_PORT_ARG_INDEX = 0;
    private static final int ARGS_NUMBER = 1;

    private static final int SUCCESS_EXIT_CODE = 0;
    private static final int FAILURE_EXIT_CODE = -1;

    public static void main(String[] args) {
        if (args.length != ARGS_NUMBER) {
            printUsage();
            System.exit(FAILURE_EXIT_CODE);
        }

        try {
            int serverPort = Integer.parseInt(args[SERVER_PORT_ARG_INDEX]);
            try (ProxyServer proxyServer = new ProxyServer(serverPort)) {
                proxyServer.run();
                System.exit(SUCCESS_EXIT_CODE);
            }
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse the server port from command line arguments: " + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        System.exit(FAILURE_EXIT_CODE);
    }

    private static void printUsage() {
        System.err.println("Usage: java <MainClassName> <server port>");
    }
}
