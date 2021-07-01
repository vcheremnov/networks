package gamemodel.logic;

import gamemodel.model.GamePlayer;
import gamemodel.model.GameState;

import java.util.Collection;

@FunctionalInterface
public interface GameStateListener {
    void notifyOfNewGameState(GameState gameState, Collection<GamePlayer> killedPlayers);
}
