package gamemodel.logic;

import gamemodel.model.GameState;
import gamemodel.model.Snake;
import gamemodel.model.auxiliary.Coordinate;
import gamemodel.model.auxiliary.Direction;
import gamemodel.model.auxiliary.FieldSize;

import java.util.*;

public class SnakeSpawnManager {
    public static final int SPAWN_PLACE_SIZE = 5;

    private GameState gameState;
    private FieldSize fieldSize;
    private Set<Coordinate> fieldCoordinates;

    private static final Direction[] directionsArray = Direction.class.getEnumConstants();
    private Set<Coordinate> spawnCoordinates = new HashSet<>(SPAWN_PLACE_SIZE * SPAWN_PLACE_SIZE);
    private Coordinate centerCoordinate;

    SnakeSpawnManager() {
        centerCoordinate = new Coordinate(SPAWN_PLACE_SIZE / 2, SPAWN_PLACE_SIZE / 2);
        for (int x = 0; x < SPAWN_PLACE_SIZE; ++x) {
            for (int y = 0; y < SPAWN_PLACE_SIZE; ++y) {
                Coordinate coordinate = new Coordinate(x, y);
                spawnCoordinates.add(coordinate);
            }
        }
    }

    void setGameState(GameState gameState) {
        this.gameState = gameState;
        fieldSize = gameState.getGameConfig().getFieldSize();
        fieldCoordinates = fieldSize.generateFieldCoordinates();
    }

    Snake spawnSnake() {
        Set<Coordinate> foodCoordinates = gameState.getFoodCoordinates();
        Set<Coordinate> availableCoordinates = getAvailableCoordinates();

        for (var headCoordinate: availableCoordinates) {
            shiftSpawnPlace(headCoordinate, fieldSize);
            if (availableCoordinates.containsAll(spawnCoordinates)) {
                List<Direction> shuffledDirections = Arrays.asList(directionsArray);
                Collections.shuffle(shuffledDirections);
                for (var direction: shuffledDirections) {
                    Coordinate tailCoordinate = Coordinate.getCoordinate(headCoordinate, direction, fieldSize);
                    if (!foodCoordinates.contains(headCoordinate) && !foodCoordinates.contains(tailCoordinate)) {
                        LinkedList<Coordinate> snakeBody = new LinkedList<>();
                        snakeBody.addFirst(headCoordinate);
                        snakeBody.addLast(tailCoordinate);
                        Direction snakeDirection = Direction.getOppositeDirection(direction);
                        return new Snake(snakeBody, snakeDirection);
                    }
                }
            }
        }

        return null;
    }

    private void shiftSpawnPlace(Coordinate newCenterCoordinate, FieldSize fieldSize) {
        int deltaX = newCenterCoordinate.getX() - centerCoordinate.getX();
        int deltaY = newCenterCoordinate.getY() - centerCoordinate.getY();
        centerCoordinate = newCenterCoordinate;

        int fieldWidth = fieldSize.getWidth();
        int fieldHeight = fieldSize.getHeight();
        for (var coordinate: spawnCoordinates) {
            int oldX = coordinate.getX();
            int oldY = coordinate.getY();
            coordinate.setX((oldX + deltaX + fieldWidth) % fieldWidth);
            coordinate.setY((oldY + deltaY + fieldHeight) % fieldHeight);
        }
    }

    private Set<Coordinate> getAvailableCoordinates() {
        Set<Coordinate> availableCoordinates = new HashSet<>(fieldCoordinates);
        for (var snake: gameState.getSnakes().values()) {
            List<Coordinate> bodyCoordinates = snake.getBodyCoordinates();
            availableCoordinates.removeAll(bodyCoordinates);
        }

        return availableCoordinates;
    }
}
