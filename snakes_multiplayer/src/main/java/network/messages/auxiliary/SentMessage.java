package network.messages.auxiliary;

import java.net.InetSocketAddress;

public class SentMessage implements Comparable<SentMessage> {
    private InetSocketAddress receiverSocketAddress;
    private Message message;
    private Long timestamp;

    public InetSocketAddress getReceiverSocketAddress() {
        return receiverSocketAddress;
    }

    public void setReceiverSocketAddress(InetSocketAddress receiverSocketAddress) {
        this.receiverSocketAddress = receiverSocketAddress;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public long getTimeSinceSent() {
        return System.currentTimeMillis() - timestamp;
    }

    public AckMessageID getAckMessageID() {
        Long messageID = message.getMessageID();
        Integer receiverID = message.getReceiverID();

        if (messageID == null || receiverID == null) {
            return null;
        }

        return new AckMessageID(messageID, receiverID);
    }

    @Override
    public int compareTo(SentMessage o) {
        return Long.compare(timestamp, o.timestamp);
    }
}