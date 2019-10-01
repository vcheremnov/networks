package server;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerLauncher {
    private static final int FAILURE_EXIT_CODE = -1;
    private static final String SHUTDOWN_COMMAND = "shutdown";

    public static void main(String[] args) {
        if (args.length != 1) {
            printUsage();
            System.exit(FAILURE_EXIT_CODE);
        }

        int serverPort = -1;
        try {
            serverPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            printUsage();
            System.exit(FAILURE_EXIT_CODE);
        }

        ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
        try (FileTransferServer server = new FileTransferServer(serverPort)){
            serverExecutor.submit(server);
            printGreetings();
            awaitShutdownCommand();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(FAILURE_EXIT_CODE);
        } finally {
            serverExecutor.shutdownNow();
        }
    }

    private static void printGreetings() {
        System.out.println("File transfer server v1.0\n" +
                "To shutdown the server, type \"shutdown\"");
    }

    private static void printUsage() {
        System.out.println("Usage: java ServerLauncher <port>");
    }

    private static void awaitShutdownCommand() throws IOException {
        try (BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in))){
            String command;
            while ((command = stdinReader.readLine()) != null) {
                if (command.equals(SHUTDOWN_COMMAND)) {
                    return;
                }
            }
        }
    }
}
