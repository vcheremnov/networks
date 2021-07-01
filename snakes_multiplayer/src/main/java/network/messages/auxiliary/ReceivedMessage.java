package network.messages.auxiliary;

import java.net.InetSocketAddress;

public class ReceivedMessage {
    private InetSocketAddress senderSocketAddress;
    private byte[] messageBytes;
    private long timestamp;

    public InetSocketAddress getSenderSocketAddress() {
        return senderSocketAddress;
    }

    public void setSenderSocketAddress(InetSocketAddress senderSocketAddress) {
        this.senderSocketAddress = senderSocketAddress;
    }

    public byte[] getMessageBytes() {
        return messageBytes;
    }

    public void setMessageBytes(byte[] messageBytes) {
        this.messageBytes = messageBytes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
