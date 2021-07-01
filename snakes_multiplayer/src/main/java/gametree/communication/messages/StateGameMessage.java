package gametree.communication.messages;

import gamemodel.model.GameState;
import gametree.communication.GameMessage;
import gametree.communication.GameMessageType;

public class StateGameMessage extends GameMessage {
    private GameState gameState;

    public StateGameMessage() {
        super(GameMessageType.STATE_MESSAGE);
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }
}
