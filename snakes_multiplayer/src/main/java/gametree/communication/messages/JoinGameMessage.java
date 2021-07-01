package gametree.communication.messages;

import gamemodel.model.auxiliary.PlayerType;
import gametree.communication.GameMessage;
import gametree.communication.GameMessageType;

public class JoinGameMessage extends GameMessage {
    private PlayerType playerType = PlayerType.HUMAN;
    private boolean onlyView = false;
    private String playerName;

    public JoinGameMessage() {
        super(GameMessageType.JOIN_MESSAGE);
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public void setPlayerType(PlayerType playerType) {
        this.playerType = playerType;
    }

    public boolean isOnlyView() {
        return onlyView;
    }

    public void setOnlyViewFlag(boolean onlyView) {
        this.onlyView = onlyView;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
