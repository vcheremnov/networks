package network.messages.auxiliary;

public class AckMessageID {
    private long messageID;
    private int nodeID;

    public AckMessageID(long messageID, int nodeID) {
        this.messageID = messageID;
        this.nodeID = nodeID;
    }

    public long getMessageID() {
        return messageID;
    }

    public int getNodeID() {
        return nodeID;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof AckMessageID)) {
            return false;
        }

        AckMessageID ackMessageID = (AckMessageID) o;
        return messageID == ackMessageID.messageID &&
                nodeID == ackMessageID.nodeID;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(nodeID) + Long.hashCode(messageID);
    }
}