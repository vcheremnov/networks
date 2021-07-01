package gamemodel.model.auxiliary;

import java.util.concurrent.ThreadLocalRandom;

public enum Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    public static Direction getRandomDirection() {
        Direction[] allDirections = Direction.class.getEnumConstants();
        int directionIndex = ThreadLocalRandom.current().nextInt(allDirections.length);
        return allDirections[directionIndex];
    }

    public static Direction getOppositeDirection(Direction direction) {
        switch (direction) {
            case UP:    return DOWN;
            case DOWN:  return UP;
            case LEFT:  return RIGHT;
            case RIGHT: return LEFT;
            default:    return null;
        }
    }

    public static Direction calculateDirection(Coordinate fromCoordinate,
                                               Coordinate toCoordinate,
                                               FieldSize fieldSize) {
        int fieldWidth = fieldSize.getWidth();
        if (fromCoordinate.getX() == fieldWidth - 1 && toCoordinate.getX() == 0) {
            return Direction.RIGHT;
        }
        if (fromCoordinate.getX() == 0 && toCoordinate.getX() == fieldWidth - 1) {
            return Direction.LEFT;
        }

        int fieldHeight = fieldSize.getHeight();
        if (fromCoordinate.getY() == fieldHeight - 1 && toCoordinate.getY() == 0) {
            return Direction.DOWN;
        }
        if (fromCoordinate.getY() == 0 && toCoordinate.getY() == fieldHeight - 1) {
            return Direction.UP;
        }

        int deltaX = toCoordinate.getX() - fromCoordinate.getX();
        if (deltaX > 0) {
            return Direction.RIGHT;
        }
        if (deltaX < 0) {
            return Direction.LEFT;
        }

        int deltaY = toCoordinate.getY() - fromCoordinate.getY();
        if (deltaY > 0) {
            return Direction.DOWN;
        }
        if (deltaY < 0) {
            return Direction.UP;
        }

        return null;
    }
};