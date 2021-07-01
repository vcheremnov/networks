package gamemodel.logic;

import gamemodel.model.GameState;
import gamemodel.model.Snake;
import gamemodel.model.auxiliary.Coordinate;
import gamemodel.model.auxiliary.Direction;
import gamemodel.model.auxiliary.FieldSize;

import java.util.LinkedList;
import java.util.Set;

public class SnakeMovementManager {
    private GameState gameState;

    void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    void shiftSnake(Snake snake) {
        FieldSize fieldSize = gameState.getGameConfig().getFieldSize();
        Set<Coordinate> foodCoordinates = gameState.getFoodCoordinates();

        Coordinate headCoordinate = snake.getHeadCoordinate();
        Direction direction = snake.getDirection();
        LinkedList<Coordinate> bodyCoordinates = snake.getBodyCoordinates();

        Coordinate newHeadCoordinate = Coordinate.getCoordinate(headCoordinate, direction, fieldSize);
        bodyCoordinates.addFirst(newHeadCoordinate);
        if (!foodCoordinates.contains(newHeadCoordinate)) {
            bodyCoordinates.removeLast();
        }
    }
}
