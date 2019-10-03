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
    private ServerSocket serverSocket;

    public FileTransferServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        SpeedMeasurer speedMeasurer = new SpeedMeasurer();
        ExecutorService threadPool = Executors.newCachedThreadPool();

        try {
            threadPool.submit(speedMeasurer);
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
