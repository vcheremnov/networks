package client;

public class ClientLauncher {
    private static final int FAILURE_EXIT_CODE = -1;

    private static final int CLI_ARGS_NUMBER = 3;
    private static final int FILENAME_ARG_INDEX = 0;
    private static final int HOSTNAME_ARG_INDEX = 1;
    private static final int PORT_ARG_INDEX = 2;

    public static void main(String[] args) {
        if (args.length != CLI_ARGS_NUMBER) {
            printUsage();
            System.exit(FAILURE_EXIT_CODE);
        }

        try {
            String filename = args[FILENAME_ARG_INDEX];
            String serverHostname = args[HOSTNAME_ARG_INDEX];
            int serverPort = Integer.parseInt(args[PORT_ARG_INDEX]);

            FileTransmission fileTransmission = new FileTransmission();
            fileTransmission.setFilepath(filename);
            fileTransmission.start(serverHostname, serverPort);

            if (fileTransmission.isSuccessful()) {
                System.out.println("File transmission has been successfully finished");
            } else {
                System.out.println("File transmission has failed");
            }
        } catch (FileTransmissionException e) {
            System.err.println(String.format("File transmission has failed with an error: %s", e.getMessage()));
            System.exit(FAILURE_EXIT_CODE);
        } catch (NumberFormatException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(FAILURE_EXIT_CODE);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java ClientLauncher <filepath> <hostname> <port>");
    }
}
