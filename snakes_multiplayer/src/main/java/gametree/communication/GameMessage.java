package gametree.communication;

public class GameMessage {
    private GameMessageType messageType;
    private Long messageID;
    private Integer receiverID;
    private Integer senderID;

    protected GameMessage(GameMessageType messageType) {
        this.messageType = messageType;
    }

    public GameMessageType getMessageType() {
        return messageType;
    }

    public Long getMessageID() {
        return messageID;
    }

    public void setMessageID(Long messageID) {
        this.messageID = messageID;
    }

    public Integer getReceiverID() {
        return receiverID;
    }

    public void setReceiverID(Integer receiverID) {
        this.receiverID = receiverID;
    }

    public Integer getSenderID() {
        return senderID;
    }

    public void setSenderID(Integer senderID) {
        this.senderID = senderID;
    }
}
