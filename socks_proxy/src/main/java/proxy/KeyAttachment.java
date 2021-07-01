package proxy;

import proxy.socks.ConnectionStatus;

import java.nio.ByteBuffer;

public class KeyAttachment {
    private static long nextID = 0;
    private static final int BUFFER_LENGTH = 8192;

    private final Long id;
    private Long remoteID;

    private boolean isClient;

    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;

    KeyAttachment(boolean isClient) {
        id = nextID++;
        this.isClient = isClient;
        readBuffer = ByteBuffer.allocate(BUFFER_LENGTH);
        writeBuffer = ByteBuffer.allocate(BUFFER_LENGTH);
    }

    public long getID() {
        return id;
    }

    public boolean isClient() {
        return isClient;
    }

    public Long getRemoteID() {
        return remoteID;
    }

    public void setRemoteID(long remoteID) {
        this.remoteID = remoteID;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }
}
