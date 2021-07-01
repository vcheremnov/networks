package gamemodel.logic;

import gamemodel.model.GameState;
import gamemodel.model.Snake;
import gamemodel.model.auxiliary.Coordinate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CollisionDetector {
    private GameState gameState;

    private Set<Coordinate> freedCoordinates = new HashSet<>();
    private Set<Integer> deadSnakes = new HashSet<>();
    private Map<Integer, Integer> frags = new HashMap<>();

    void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    void calculateCollisions() {
        clearData();

        var snakes = gameState.getSnakes();
        for (var curSnakeID: snakes.keySet()) {
            Snake curSnake = snakes.get(curSnakeID);
            for (var otherSnakeID: snakes.keySet()) {
                Snake otherSnake = snakes.get(otherSnakeID);
                if (hasCrashedInto(curSnake, otherSnake)) {
                    addFrag(otherSnakeID);
                    freedCoordinates.addAll(curSnake.getBodyCoordinates());
                    deadSnakes.add(curSnakeID);
                    break;
                }
            }
        }

        frags.keySet().removeAll(deadSnakes);
        for (var snakeID: snakes.keySet()) {
            Snake snake = snakes.get(snakeID);
            if (!deadSnakes.contains(snakeID)) {
                freedCoordinates.removeAll(snake.getBodyCoordinates());
            }
        }
    }

    Set<Integer> getDeadSnakes() {
        return deadSnakes;
    }

    Map<Integer, Integer> getFrags() {
        return frags;
    }

    Set<Coordinate> getFreedCoordinates() {
        return freedCoordinates;
    }

    private boolean hasCrashedInto(Snake snake, Snake otherSnake) {
        var otherSnakeBodyIterator = otherSnake.getBodyCoordinates().iterator();
        if (snake == otherSnake) {
            // skip head coordinate
            otherSnakeBodyIterator.next();
        }

        var headCoordinate = snake.getHeadCoordinate();
        while (otherSnakeBodyIterator.hasNext()) {
            var otherSnakeBodyCoordinate = otherSnakeBodyIterator.next();
            if (headCoordinate.equals(otherSnakeBodyCoordinate)) {
                return true;
            }
        }

        return false;
    }

    private void addFrag(int playerID) {
        int currentFragsNumber = frags.getOrDefault(playerID, 0);
        frags.put(playerID, currentFragsNumber + 1);
    }

    private void clearData() {
        freedCoordinates.clear();
        deadSnakes.clear();
        frags.clear();
    }
}
