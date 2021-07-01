package gamemodel.logic;

import gamemodel.model.auxiliary.Coordinate;
import gamemodel.model.auxiliary.Direction;
import gamemodel.model.auxiliary.PlayerAdditionResult;
import gamemodel.model.GameConfig;
import gamemodel.model.GamePlayer;
import gamemodel.model.GameState;
import gamemodel.model.Snake;
import gametree.NodeRole;

import java.util.*;

public class GameStateController implements Runnable {
    private static final int POINTS_PER_FOOD = 1;
    private static final int POINTS_PER_KILLED_SNAKE = 1;

    private SnakeMovementManager snakeMovementManager = new SnakeMovementManager();
    private CollisionDetector collisionDetector = new CollisionDetector();
    private FoodGenerator foodGenerator = new FoodGenerator();
    private SnakeSpawnManager snakeSpawnManager = new SnakeSpawnManager();

    private GameStateListener gameStateListener;
    private GameConfig gameConfig;
    private GameState gameState;

    public synchronized boolean isGameStateAbsent() {
        return gameState == null;
    }

    public synchronized GameState getGameState() throws InterruptedException {
        while (isGameStateAbsent()) {
            wait();
        }

        return gameState;
    }

    public synchronized void setGameState(GameState gameState) {
        gameConfig = gameState.getGameConfig();
        this.gameState = gameState;

        snakeMovementManager.setGameState(gameState);
        collisionDetector.setGameState(gameState);
        foodGenerator.setGameState(gameState);
        snakeSpawnManager.setGameState(gameState);

        foodGenerator.spawnFood();

        notifyAll();
    }

    public synchronized void setGameStateListener(GameStateListener gameStateListener) {
        this.gameStateListener = gameStateListener;
    }

    public synchronized void clearGameState() {
        gameState = null;
        gameConfig = null;
    }

    public synchronized PlayerAdditionResult addPlayer(GamePlayer newPlayer) {
        if (isGameStateAbsent()) {
            return PlayerAdditionResult.NOT_MASTER_ERROR;
        }

        synchronized (gameState) {
            int playerID = newPlayer.getPlayerID();
            var players = gameState.getPlayers();
            var snakes = gameState.getSnakes();

            if (players.containsKey(playerID)) {
                return PlayerAdditionResult.ID_EXISTS_ERROR;
            }

            if (newPlayer.getNodeRole() != NodeRole.VIEWER) {
                Snake snake = snakeSpawnManager.spawnSnake();
                if (snake == null) {
                    return PlayerAdditionResult.NO_PLACE_ERROR;
                }
                snakes.put(playerID, snake);
            }

            players.put(playerID, newPlayer);
        }

        return PlayerAdditionResult.SUCCESS;
    }

    public synchronized void removePlayer(int playerID) {
        if (isGameStateAbsent()) {
            return;
        }

        synchronized (gameState) {
            gameState.getPlayers().remove(playerID);
            var snake = gameState.getSnakes().get(playerID);
            if (snake != null) {
                snake.makeZombie();
            }
        }
    }

    public synchronized void setDirection(int playerID, Direction direction) {
        if (isGameStateAbsent()) {
            throw new RuntimeException("Game state was not set");
        }

        synchronized (gameState) {
            Snake snake = gameState.getSnakes().get(playerID);
            if (snake != null) {
                snake.setDirection(direction);
            }
        }
    }

    @Override
    public void run() {
        try {
            long nextStateDelay;
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (this) {
                    while (isGameStateAbsent()) {
                        wait();
                    }
                    nextStateDelay = gameConfig.getStateDelayMillis();
                }

                Thread.sleep(nextStateDelay);
                calculateNextState();
            }
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void calculateNextState() {
        if (isGameStateAbsent()) {
            return;
        }

        Collection<GamePlayer> killedPlayers;
        GameState currentGameState = gameState;
        synchronized (currentGameState) {
            shiftSnakes();
            calculateCollisions();
            killedPlayers = calculateKilledPlayers();
            spawnFood();
            currentGameState.incrementStateOrder();

            System.err.println(currentGameState.getStateOrder());

            boolean stopIsNeeded = isStopNeeded(killedPlayers);
            if (stopIsNeeded) {
                clearGameState();
            }
        }

        if (gameStateListener != null) {
            gameStateListener.notifyOfNewGameState(currentGameState, killedPlayers);
        }
    }

    private boolean isStopNeeded(Collection<GamePlayer> killedPlayers) {
        boolean masterHasDied = gameState.getPlayers().values().stream()
                .noneMatch(player -> player.getNodeRole() == NodeRole.MASTER);

        boolean gameHasFinished = killedPlayers.size() > 0 &&
                gameState.getPlayers().values().stream().
                noneMatch(player -> player.getNodeRole() != NodeRole.VIEWER);

        return masterHasDied || gameHasFinished;
    }

    private void shiftSnakes() {
        var snakes = gameState.getSnakes();
        var foodCoordinates = gameState.getFoodCoordinates();

        for (var playerID: snakes.keySet()) {
            Snake snake = snakes.get(playerID);
            snakeMovementManager.shiftSnake(snake);

            Coordinate headCoordinate = snake.getHeadCoordinate();
            boolean foodWasEaten = foodCoordinates.remove(headCoordinate);
            if (foodWasEaten) {
                increasePlayerScore(playerID, POINTS_PER_FOOD);
            }
        }
    }

    private void calculateCollisions() {
        collisionDetector.calculateCollisions();
        Map<Integer, Integer> frags = collisionDetector.getFrags();

        for (var playerID: frags.keySet()) {
            increasePlayerScore(playerID, frags.get(playerID) * POINTS_PER_KILLED_SNAKE);
        }
    }

    private void spawnFood() {
        Set<Coordinate> freedCoordinates = collisionDetector.getFreedCoordinates();
        foodGenerator.spawnFood(freedCoordinates);
        foodGenerator.spawnFood();
    }

    private Collection<GamePlayer> calculateKilledPlayers() {
        var players = gameState.getPlayers();
        var snakes = gameState.getSnakes();

        Collection<GamePlayer> killedPlayers = new ArrayList<>();
        Set<Integer> deadSnakes = collisionDetector.getDeadSnakes();

        for (var playerID: deadSnakes) {
            GamePlayer killedPlayer = players.get(playerID);
            killedPlayer.setNodeRole(NodeRole.VIEWER);
            killedPlayers.add(killedPlayer);
            snakes.remove(playerID);
        }

        return killedPlayers;
    }

    private void increasePlayerScore(int playerID, int points) {
        GamePlayer player = gameState.getPlayers().get(playerID);
        if (player != null) {
            int newScore = player.getScore() + points;
            player.setScore(newScore);
        }
    }
}
