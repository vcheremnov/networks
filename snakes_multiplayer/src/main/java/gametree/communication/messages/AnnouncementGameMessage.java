package gametree.communication.messages;

import gamemodel.model.GameConfig;
import gamemodel.model.GamePlayer;
import gametree.communication.GameMessage;
import gametree.communication.GameMessageType;

import java.util.Collection;

public class AnnouncementGameMessage extends GameMessage {
    private Collection<GamePlayer> players;
    private boolean canJoin = true;
    private GameConfig gameConfig;

    public AnnouncementGameMessage() {
        super(GameMessageType.ANNOUNCEMENT_MESSAGE);
    }

    public Collection<GamePlayer> getPlayers() {
        return players;
    }

    public void setPlayers(Collection<GamePlayer> players) {
        this.players = players;
    }

    public boolean canJoin() {
        return canJoin;
    }

    public void setCanJoinFlag(boolean canJoin) {
        this.canJoin = canJoin;
    }

    public GameConfig getGameConfig() {
        return gameConfig;
    }

    public void setGameConfig(GameConfig gameConfig) {
        this.gameConfig = gameConfig;
    }
}
