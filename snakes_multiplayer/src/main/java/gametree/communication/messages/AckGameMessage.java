package gametree.communication.messages;

import gametree.communication.GameMessage;
import gametree.communication.GameMessageType;

public class AckGameMessage extends GameMessage {
    public AckGameMessage() {
        super(GameMessageType.ACK_MESSAGE);
    }
}
