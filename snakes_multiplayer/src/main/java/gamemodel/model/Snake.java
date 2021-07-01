package gamemodel.model;

import gamemodel.model.auxiliary.Coordinate;
import gamemodel.model.auxiliary.Direction;

import java.util.*;

public class Snake {
    private boolean isZombie = false;
    private Direction direction;
    private LinkedList<Coordinate> bodyCoordinates;

    public Snake(LinkedList<Coordinate> bodyCoordinates, Direction direction) {
        this.direction = direction;
        this.bodyCoordinates = bodyCoordinates;
    }

    public boolean isZombie() {
        return isZombie;
    }

    public void makeZombie() {
        isZombie = true;
    }

    public void setDirection(Direction newDirection) {
        if (isZombie) {
            return;
        }

        Direction oppositeDirection = Direction.getOppositeDirection(direction);
        if (newDirection != oppositeDirection) {
            direction = newDirection;
        }
    }

    public Direction getDirection() {
        return direction;
    }

    public Coordinate getHeadCoordinate() {
        return bodyCoordinates.getFirst();
    }

    public LinkedList<Coordinate> getBodyCoordinates() {
        return bodyCoordinates;
    }
}