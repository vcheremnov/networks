package gametree.communication.messages;

import gametree.communication.GameMessage;
import gametree.communication.GameMessageType;

public class PingGameMessage extends GameMessage {
    public PingGameMessage() {
        super(GameMessageType.PING_MESSAGE);
    }
}
