package gametree.communication.messages;

import gamemodel.model.auxiliary.Direction;
import gametree.communication.GameMessage;
import gametree.communication.GameMessageType;

public class SteerGameMessage extends GameMessage {
    private Direction direction;

    public SteerGameMessage() {
        super(GameMessageType.STEER_MESSAGE);
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }
}
