package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileTransferServer {
    private static final String DOWNLOAD_DIR = "uploads";

    private int port;
    private ServerSocket serverSocket;
    private List<Socket> activeConnections;
    private ExecutorService threadPool;

    public FileTransferServer(int port) throws IOException {
        this.port = port;
        serverSocket = new ServerSocket(port);
        activeConnections = new ArrayList<>();
    }

    public void start() throws IOException {
        threadPool = Executors.newCachedThreadPool();
        while (!Thread.currentThread().isInterrupted()) {
            Socket socket = serverSocket.accept();
            ConnectionHandler connectionHandler = new ConnectionHandler(socket);
            threadPool.submit(connectionHandler);
        }
    }

    public void close() {
        threadPool.shutdownNow();

        for (Socket connection: activeConnections) {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
