package proto;

import gamemodel.model.auxiliary.Coordinate;
import gamemodel.model.auxiliary.Direction;
import gamemodel.model.auxiliary.FieldSize;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class SnakeBodyConverter {
    public static ArrayList<Coordinate> getKeyCoordinates(LinkedList<Coordinate> bodyCoordinates,
                                                          FieldSize fieldSize) {
        ArrayList<Coordinate> keyCoordinates = new ArrayList<>();

        Iterator<Coordinate> iterator = bodyCoordinates.iterator();
        Coordinate prevCoordinate = iterator.next();
        Direction prevDirection = null;
        while (iterator.hasNext()) {
            Coordinate curCoordinate = iterator.next();
            Direction curDirection = Direction.calculateDirection(prevCoordinate, curCoordinate, fieldSize);
            if (prevDirection != curDirection) {
                prevDirection = curDirection;
                keyCoordinates.add(prevCoordinate);
            }
            prevCoordinate = curCoordinate;
        }

        Coordinate tailCoordinate = bodyCoordinates.getLast();
        keyCoordinates.add(tailCoordinate);

        return keyCoordinates;
    }

    public static LinkedList<Coordinate> getBodyCoordinates(ArrayList<Coordinate> keyCoordinates,
                                                            FieldSize fieldSize) {
        return null;
    }
}
