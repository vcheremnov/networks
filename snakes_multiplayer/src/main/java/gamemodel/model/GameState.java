package gamemodel.model;

import gamemodel.model.auxiliary.Coordinate;
import gui.Observable;

import java.util.*;

public class GameState extends Observable {
    private static final int INITIAL_STATE_ORDER = 0;

    private Map<Integer, GamePlayer> players = new HashMap<>();
    private Map<Integer, Snake> snakes = new HashMap<>();
    private Set<Coordinate> foodCoordinates = new HashSet<>();
    private GameConfig gameConfig;

    private int stateOrder = INITIAL_STATE_ORDER;

    public static class Property extends Observable.Property {
        public static final Property GAME_STATE_CHANGED = new Property("GAME_STATE_CHANGED");

        protected Property(String name) {
            super(name);
        }
    }

    public GameState(GameConfig gameConfig) {
        this.gameConfig = gameConfig;
    }

    public Map<Integer, Snake> getSnakes() {
        return snakes;
    }

    public Map<Integer, GamePlayer> getPlayers() {
        return players;
    }

    public Set<Coordinate> getFoodCoordinates() {
        return foodCoordinates;
    }

    public GameConfig getGameConfig() {
        return gameConfig;
    }

    public int getStateOrder() {
        return stateOrder;
    }

    public void setStateOrder(int stateOrder) {
        this.stateOrder = stateOrder;
    }

    public void incrementStateOrder() {
        ++stateOrder;
        firePropertyChanged(Property.GAME_STATE_CHANGED, null, this);
    }
}
