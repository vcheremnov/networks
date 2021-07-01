package gamemodel.model.auxiliary;

public class Coordinate {
    private int x;
    private int y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;
        // null check
        if (o == null)
            return false;
        // type check and cast
        if (getClass() != o.getClass())
            return false;
        Coordinate coordinate = (Coordinate) o;
        // field comparison
        return x == coordinate.x && y == coordinate.y;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(x) ^ Integer.hashCode(y);
    }

    public static Coordinate getCoordinate(Coordinate baseCoordinate, Direction direction, FieldSize fieldSize) {
        int fieldWidth = fieldSize.getWidth();
        int fieldHeight = fieldSize.getHeight();

        int newCoordinateX = baseCoordinate.getX();
        int newCoordinateY = baseCoordinate.getY();
        switch (direction) {
            case UP:
                newCoordinateY = (newCoordinateY + fieldHeight - 1) % fieldHeight;
                break;
            case DOWN:
                newCoordinateY = (newCoordinateY + 1) % fieldHeight;
                break;
            case LEFT:
                newCoordinateX = (newCoordinateX + fieldWidth - 1) % fieldWidth;
                break;
            case RIGHT:
                newCoordinateX = (newCoordinateX + 1) % fieldWidth;
                break;
        }

        return new Coordinate(newCoordinateX, newCoordinateY);
    }
}
