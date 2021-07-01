package gametree;

import gamemodel.model.GameConfig;
import gamemodel.model.GamePlayer;
import gamemodel.model.GameState;
import gamemodel.logic.GameStateController;
import gamemodel.model.auxiliary.Direction;
import gamemodel.model.auxiliary.PlayerAdditionResult;
import gui.Observable;

import java.util.*;

public class GameTreeNodeData extends Observable {
    private static final int INITIAL_PLAYER_ID = 0;
    private static final int INVALID_PLAYER_ID = -1;
    private static final String DEFAULT_PLAYER_NAME = "SnakePlayer";

    private int playerID;
    private NodeRole playerNodeRole;
    private String playerName = DEFAULT_PLAYER_NAME;

    private Integer masterPlayerID;
    private Integer deputyPlayerID;

    private boolean isGameStarted;
    private GameState gameState;
    private final GameConfig ownGameConfig;
    private GameConfig currentGameConfig;
    private GameStateController gameStateController;

    private int nextPlayerID = 0;

    public static class Property extends Observable.Property {
        public static final Property GAME_STATE_CHANGED = new Property("GAME_STATE_CHANGED");
        public static final Property GAME_STARTED = new Property("GAME_STARTED");
        public static final Property DISCONNECTED = new Property("DISCONNECTED");
        public static final Property MASTER_CHANGED = new Property("MASTER_CHANGED");

        public static final Property SERVER_INFO_GOT = new Property("SERVER_INFO_GOT");
        public static final Property SERVER_INFO_REMOVED = new Property("SERVER_INFO_REMOVED");

        public static final Property INFO_MESSAGE_GOT = new Property("INFO_MESSAGE_GOT");

        protected Property(String name) {
            super(name);
        }
    }

    public GameTreeNodeData(GameConfig gameConfig, GameStateController gameStateController) {
        ownGameConfig = gameConfig;
        this.gameStateController = gameStateController;
        resetData();
    }

    public synchronized void addServerInfo(ServerInfo serverInfo) {
        firePropertyChanged(Property.SERVER_INFO_GOT, null, serverInfo);
    }

    public synchronized void removeServerInfo(ServerInfo serverInfo) {
        firePropertyChanged(Property.SERVER_INFO_REMOVED, null, serverInfo);
    }

    public synchronized void addInfoMessage(String infoMessage) {
        firePropertyChanged(Property.INFO_MESSAGE_GOT, null, infoMessage);
    }

    public synchronized Integer getMasterPlayerID() {
        return masterPlayerID;
    }

    public synchronized Integer getDeputyPlayerID() {
        return deputyPlayerID;
    }

    public synchronized String getPlayerName() {
        return playerName;
    }

    public synchronized void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public synchronized int getPlayerID() {
        return playerID;
    }

    public synchronized void setPlayerID(int playerID) {
        this.playerID = playerID;
    }

    public synchronized boolean isGameStarted() {
        return isGameStarted;
    }

    public synchronized GameState getGameState() {
        return gameState;
    }

    public synchronized GameConfig getGameConfig() {
        return currentGameConfig;
    }

    public synchronized boolean setGameState(GameState newGameState) {
        if (playerNodeRole == NodeRole.MASTER) {
            throw new RuntimeException("Tried to set a game state while being a master node");
        }

        if (gameState != null && newGameState.getStateOrder() <= gameState.getStateOrder()) {
            return false;
        }

        gameState = newGameState;
        currentGameConfig = gameState.getGameConfig();
        var playerIdSet = gameState.getPlayers().keySet();
        playerIdSet.stream().max(Integer::compareTo).ifPresent(id -> {
            if (id >= nextPlayerID) {
                nextPlayerID = id + 1;
            }
        });

        firePropertyChanged(Property.GAME_STATE_CHANGED, null, gameState);
        return true;
    }

    public synchronized void assignRoleToPlayer(NodeRole role, Integer playerID) {
        if (playerID != null) {
            if (gameState != null) {
                synchronized (gameState) {
                    var players = gameState.getPlayers();
                    if (players.containsKey(playerID)) {
                        players.get(playerID).setNodeRole(role);
                    }
                }
            }

            if (playerID.equals(this.playerID)) {
                playerNodeRole = role;
                if (role == NodeRole.MASTER) {
                    if (gameState == null) {
                        throw new RuntimeException(
                                "Role changed to master, but game state hasn't been got yet"
                        );
                    }

                    gameState.addPropertyChangeListener(
                            GameState.Property.GAME_STATE_CHANGED,
                            evt -> firePropertyChanged(Property.GAME_STATE_CHANGED, null, gameState)
                    );
                    gameStateController.setGameState(gameState);
                }
            }
        }

        if (role == NodeRole.MASTER) {
            masterPlayerID = playerID;
            firePropertyChanged(Property.MASTER_CHANGED, null, null);
        } else if (role == NodeRole.DEPUTY) {
            deputyPlayerID = playerID;
        }
    }

    public synchronized PlayerAdditionResult addPlayer(GamePlayer player) {
        if (playerNodeRole != NodeRole.MASTER) {
            throw new RuntimeException("Tried to add player while being a non-master node");
        }

        player.setPlayerID(getNextPlayerID());
        return gameStateController.addPlayer(player);
    }

    public synchronized void removePlayer(int playerID) {
        if (playerNodeRole != NodeRole.MASTER) {
            throw new RuntimeException("Tried to remove player while being a non-master node");
        }

        gameStateController.removePlayer(playerID);
    }

    public synchronized void setPlayerDirection(int playerID, Direction direction) {
        if (playerNodeRole != NodeRole.MASTER) {
            throw new RuntimeException("Tried to set player direction while being a non-master node");
        }

        gameStateController.setDirection(playerID, direction);
    }

    public synchronized NodeRole getPlayerNodeRole() {
        return playerNodeRole;
    }

    public synchronized void startGame() {
        isGameStarted = true;
        firePropertyChanged(Property.GAME_STARTED, null, gameState);
    }

    public synchronized void leaveGame() {
        resetData();
        firePropertyChanged(Property.DISCONNECTED, null, null);
    }

    public synchronized void startOwnGame(int ownPort) {
        gameState = new GameState(ownGameConfig);
        gameState.addPropertyChangeListener(
                GameState.Property.GAME_STATE_CHANGED,
                evt -> firePropertyChanged(Property.GAME_STATE_CHANGED, null, gameState)
        );

        playerID = getNextPlayerID();
        assignRoleToPlayer(NodeRole.MASTER, playerID);

        GamePlayer player = new GamePlayer();
        player.setName(playerName);
        player.setPort(ownPort);
        player.setAddress("");
        player.setNodeRole(NodeRole.MASTER);
        player.setPlayerID(playerID);
        gameStateController.addPlayer(player);

        startGame();
    }

    public synchronized void joinGame(ServerInfo serverInfo, boolean isViewer) {
        playerNodeRole = isViewer ? NodeRole.VIEWER : NodeRole.NORMAL;
        masterPlayerID = serverInfo.getMasterID();
        deputyPlayerID = serverInfo.getDeputyID();
        currentGameConfig = serverInfo.getGameConfig();
    }

    public synchronized void resetData() {
        playerID = INVALID_PLAYER_ID;
        nextPlayerID = INITIAL_PLAYER_ID;

        isGameStarted = false;
        gameStateController.clearGameState();
        currentGameConfig = ownGameConfig;
        gameState = null;
        deputyPlayerID = null;
        masterPlayerID = null;
        playerNodeRole = null;
    }

    private synchronized int getNextPlayerID() {
        return nextPlayerID++;
    }
}
