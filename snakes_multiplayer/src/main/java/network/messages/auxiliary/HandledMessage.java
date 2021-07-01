package network.messages.auxiliary;

public class HandledMessage {
    private Message message;
    private Boolean isAckMessage;
    private Long receiveTimestamp;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Long getReceiveTimestamp() {
        return receiveTimestamp;
    }

    public void setReceiveTimestamp(Long receiveTimestamp) {
        this.receiveTimestamp = receiveTimestamp;
    }

    public Boolean isAckMessage() {
        return isAckMessage;
    }

    public void setAckMessageFlag(Boolean isAckMessage) {
        this.isAckMessage = isAckMessage;
    }

    public AckMessageID getAckMessageID() {
        Long messageID = message.getMessageID();
        Integer senderID = message.getSenderID();

        if (!isAckMessage || messageID == null || senderID == null) {
            return null;
        }

        return new AckMessageID(messageID, senderID);
    }
}
