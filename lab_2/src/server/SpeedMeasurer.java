package server;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedMeasurer implements Runnable {
    private static final long MEASURE_DELAY_SECS = 3;
    private static final long MILLIS_PER_SEC = 1000;

    private final ConcurrentHashMap<ConnectionManager, Long> downloadedBytes = new ConcurrentHashMap<>();

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(MEASURE_DELAY_SECS * MILLIS_PER_SEC);
                clearScreen();

                Iterator<ConnectionManager> it = downloadedBytes.keySet().iterator();
                while (it.hasNext()) {
                    ConnectionManager connectionManager = it.next();
                    if (!connectionManager.isActive()) {
                        it.remove();
                    }

                    ConnectionInfo connectionInfo = calculateConnectionInfo(connectionManager);
                    printConnectionInfo(connectionInfo);
                }

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void submitConnectionManager(ConnectionManager connectionManager) {
        downloadedBytes.putIfAbsent(connectionManager, 0L);
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private ConnectionInfo calculateConnectionInfo(ConnectionManager connectionManager) {
        long curTotalBytesReceived = connectionManager.getTotalBytesReceived();
        long prevTotalBytesReceived = downloadedBytes.put(connectionManager, curTotalBytesReceived);
        long downloadDuration = connectionManager.getDownloadDuration();

        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.setRemoteAddress(connectionManager.getRemoteAddress());
        connectionInfo.setRemotePort(connectionManager.getRemotePort());
        connectionInfo.setFileLength(connectionManager.getExpectedFileLength());
        connectionInfo.setTotalBytesDownloaded(curTotalBytesReceived);
        connectionInfo.setBytesPerSecond((curTotalBytesReceived - prevTotalBytesReceived) / MEASURE_DELAY_SECS);
        connectionInfo.setAverageBytesPerSecond(
                (downloadDuration == 0) ? curTotalBytesReceived : (curTotalBytesReceived / downloadDuration)
        );

        return connectionInfo;
    }

    private void printConnectionInfo(ConnectionInfo connectionInfo) {
        String remoteAddress = String.format("%s:%d", connectionInfo.getRemoteAddress(), connectionInfo.getRemotePort());
        long bytesPerSecond = connectionInfo.getBytesPerSecond();
        long averageBytesPerSecond = connectionInfo.getAverageBytesPerSecond();
        long bytesDownloaded = connectionInfo.getTotalBytesDownloaded();

        long fileLength = connectionInfo.getFileLength();
        String expectedFileLength = (fileLength >= 0) ? Long.toString(fileLength) : "???";

        System.out.println(String.format("%s: %d B/s current, %d B/s average, %d/%s bytes downloaded",
                remoteAddress, bytesPerSecond, averageBytesPerSecond, bytesDownloaded, expectedFileLength));
    }
}
