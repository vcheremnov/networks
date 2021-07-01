import chatnode.ChatNode;

import java.net.InetSocketAddress;

public class Main {
    private static final int FAILURE_EXIT_CODE = -1;

    private static final int CLI_REQUIRED_ONLY_ARGS_NUMBER = 4;
    private static final int CLI_ALL_ARGS_NUMBER = 6;

    private static final int NODE_NAME_ARG_INDEX = 0;
    private static final int NODE_HOSTNAME_ARG_INDEX = 1;
    private static final int NODE_PORT_ARG_INDEX = 2;
    private static final int LOSS_PERCENTAGE_ARG_INDEX = 3;
    private static final int PARENT_HOSTNAME_ARG_INDEX = 4;
    private static final int PARENT_PORT_ARG_INDEX = 5;

    public static void main(String[] args) {
        if (args.length != CLI_REQUIRED_ONLY_ARGS_NUMBER &&
            args.length != CLI_ALL_ARGS_NUMBER) {
            printUsage();
            System.exit(FAILURE_EXIT_CODE);
        }

        try {
            String nodeName = args[NODE_NAME_ARG_INDEX];
            String nodeHostname = args[NODE_HOSTNAME_ARG_INDEX];
            int nodePort = Integer.parseInt(args[NODE_PORT_ARG_INDEX]);
            int lossPercentage = Integer.parseInt(args[LOSS_PERCENTAGE_ARG_INDEX]);
            if (lossPercentage < 0 || lossPercentage > 100) {
                System.err.println("Error: loss percentage has to be a value from 0 to 100");
                System.exit(FAILURE_EXIT_CODE);
            }

            ChatNode chatNode = new ChatNode(nodeName);

            if (args.length == CLI_ALL_ARGS_NUMBER) {
                String parentHostname = args[PARENT_HOSTNAME_ARG_INDEX];
                int parentPort = Integer.parseInt(args[PARENT_PORT_ARG_INDEX]);
                chatNode.setParent(new InetSocketAddress(parentHostname, parentPort));
            }

            chatNode.start(nodeHostname, nodePort, lossPercentage);
        } catch (NumberFormatException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(FAILURE_EXIT_CODE);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

    }

    private static void printUsage() {
        System.err.println("Usage: java Main <node name> <node hostname> <node port> <loss %> " +
                "[<parent node hostname> <parent node port>]");
    }
}
