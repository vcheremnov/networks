package gametree.communication.messages;

import gametree.NodeRole;
import gametree.communication.GameMessage;
import gametree.communication.GameMessageType;

public class RoleChangeGameMessage extends GameMessage {
    private NodeRole senderRole;
    private NodeRole receiverRole;

    public RoleChangeGameMessage() {
        super(GameMessageType.ROLE_CHANGE_MESSAGE);
    }

    public NodeRole getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(NodeRole senderRole) {
        this.senderRole = senderRole;
    }

    public NodeRole getReceiverRole() {
        return receiverRole;
    }

    public void setReceiverRole(NodeRole receiverRole) {
        this.receiverRole = receiverRole;
    }
}
