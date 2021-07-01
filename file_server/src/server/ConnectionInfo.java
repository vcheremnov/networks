package server;

import java.net.InetAddress;

public class ConnectionInfo {
    private InetAddress remoteAddress;
    private int remotePort;

    private long totalBytesDownloaded;
    private long fileLength;

    private long bytesPerSecond;
    private long averageBytesPerSecond;


    public long getTotalBytesDownloaded() {
        return totalBytesDownloaded;
    }

    public void setTotalBytesDownloaded(long totalBytesDownloaded) {
        this.totalBytesDownloaded = totalBytesDownloaded;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public long getBytesPerSecond() {
        return bytesPerSecond;
    }

    public void setBytesPerSecond(long bytesPerSecond) {
        this.bytesPerSecond = bytesPerSecond;
    }

    public long getAverageBytesPerSecond() {
        return averageBytesPerSecond;
    }

    public void setAverageBytesPerSecond(long averageBytesPerSecond) {
        this.averageBytesPerSecond = averageBytesPerSecond;
    }

    public InetAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }
}
