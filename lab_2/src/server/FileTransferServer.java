package server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileTransferServer implements Runnable, Closeable {
    private int port;
    private ServerSocket serverSocket;
    private HashMap<ConnectionManager, Long> ;

    public FileTransferServer(int port) throws IOException {
        this.port = port;
        serverSocket = new ServerSocket(port);
        connectionHandlers = new ArrayList<>();
    }

    @Override
    public void run() {
        SpeedMeasurer speedMeasurer = new SpeedMeasurer();
        ExecutorService threadPool = Executors.newCachedThreadPool();

        try {
            speedMeasurer.start();
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                ConnectionManager connectionManager = new ConnectionManager(socket);
                speedMeasurer.submitConnectionManager(connectionManager);
                threadPool.submit(connectionManager);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            threadPool.shutdownNow();
            speedMeasurer.stop();
        }
    }

    @Override
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }
}
