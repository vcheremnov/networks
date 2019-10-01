package server;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpeedMeasurer {
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final long MEASURE_DELAY_MILLIS = 1000;
    private static final long MILLIS_PER_SEC = 1000;

    private boolean isStarted = false;
    private final HashMap<ConnectionManager, Long> downloadedBytes = new HashMap<>();

    public void start() {
        synchronized (this) {
            if (isStarted) {
                throw new RuntimeException("SpeedMeasurer instance has been already started");
            }
            isStarted = true;
        }

        executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(MEASURE_DELAY_MILLIS);
                    clearScreen();

                    Iterator<ConnectionManager> it = downloadedBytes.keySet().iterator();
                    while (it.hasNext()) {
                        ConnectionManager connectionManager = it.next();
                        if (!connectionManager.isActive()) {
                            it.remove();
                        }

                        long curTotalBytesReceived = connectionManager.getTotalBytesReceived();
                        long prevTotalBytesReceived = downloadedBytes.put(connectionManager, curTotalBytesReceived);
                        long bytesDownloaded = curTotalBytesReceived - prevTotalBytesReceived;
                        double downloadSpeed = (double) bytesDownloaded / MEASURE_DELAY_MILLIS * MILLIS_PER_SEC;
                        printConnectionInfo(connectionManager, downloadSpeed, bytesDownloaded);
                    }

                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void stop() {
        executor.shutdownNow();
    }

    public void submitConnectionManager(ConnectionManager connectionManager) {
        synchronized (downloadedBytes) {
            downloadedBytes.putIfAbsent(connectionManager, 0L);
        }
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void printConnectionInfo(ConnectionManager connectionManager, double downloadSpeed, long bytesDownloaded) {
        String remoteAddress = connectionManager.getRemoteAddress().toString();
        long expectedFileLength = connectionManager.getExpectedFileLength();
        System.out.println(String.format("%s: %.2f B/s, downloaded %d/%s",
                remoteAddress, downloadSpeed, bytesDownloaded, expectedFileLength >= 0 ? expectedFileLength : "?"));
    }
}
