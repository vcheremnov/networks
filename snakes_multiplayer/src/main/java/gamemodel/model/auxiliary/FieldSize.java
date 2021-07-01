package gamemodel.model.auxiliary;

import java.util.HashSet;
import java.util.Set;

public class FieldSize {
    private int width;
    private int height;

    public FieldSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Set<Coordinate> generateFieldCoordinates() {
        Set<Coordinate> fieldCoordinates = new HashSet<>(width * height);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                fieldCoordinates.add(new Coordinate(x, y));
            }
        }

        return fieldCoordinates;
    }
}
