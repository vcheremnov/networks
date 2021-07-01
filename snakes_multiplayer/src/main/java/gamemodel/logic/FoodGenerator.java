package gamemodel.logic;

import gamemodel.model.GameConfig;
import gamemodel.model.GameState;
import gamemodel.model.auxiliary.Coordinate;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FoodGenerator {
    private GameState gameState;
    private GameConfig gameConfig;
    private Set<Coordinate> fieldCoordinates;

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        gameConfig = gameState.getGameConfig();
        fieldCoordinates = gameConfig.getFieldSize().generateFieldCoordinates();
    }

    void spawnFood() {
        var foodCoordinates = gameState.getFoodCoordinates();
        int actualFoodAmount = foodCoordinates.size();
        int requiredFoodAmount = getRequiredFoodAmount();
        if (requiredFoodAmount <= actualFoodAmount) {
            return;
        }

        var availableCoordinates = getAvailableCoordinates();
        int foodToGenerate = Math.min(requiredFoodAmount - actualFoodAmount, availableCoordinates.size());

        List<Coordinate> availableCoordinatesList = new LinkedList<>(availableCoordinates);
        Collections.shuffle(availableCoordinatesList);

        Set<Coordinate> generatedFoodCoordinates = new HashSet<>(availableCoordinatesList.subList(0, foodToGenerate));
        foodCoordinates.addAll(generatedFoodCoordinates);
    }

    void spawnFood(Set<Coordinate> deadSnakesCoordinates) {
        var foodCoordinates = gameState.getFoodCoordinates();
        var availableCoordinates = getAvailableCoordinates();

        for (var coordinate: deadSnakesCoordinates) {
            float randomFloat = ThreadLocalRandom.current().nextFloat();
            if (randomFloat >= gameConfig.getDeadFoodProbability()) {
                continue;
            }

            boolean cellIsEmpty = availableCoordinates.contains(coordinate);
            if (cellIsEmpty) {
                foodCoordinates.add(coordinate);
            }
        }
    }

    private int getRequiredFoodAmount() {
        int staticFoodAmount = gameConfig.getStaticFoodAmount();
        float foodPerPlayer = gameConfig.getFoodPerPlayer();
        int aliveSnakesNumber = countAliveSnakes();
        return staticFoodAmount + (int) (foodPerPlayer * aliveSnakesNumber);
    }

    private int countAliveSnakes() {
        var snakes = gameState.getSnakes();

        int aliveSnakesNumber = 0;
        for (var snake: snakes.values()) {
            if (snake.isZombie()) {
                continue;
            }
            ++aliveSnakesNumber;
        }

        return aliveSnakesNumber;
    }

    private Set<Coordinate> getAvailableCoordinates() {
        Set<Coordinate> availableCoordinates = new HashSet<>(fieldCoordinates);
        for (var snake: gameState.getSnakes().values()) {
            List<Coordinate> bodyCoordinates = snake.getBodyCoordinates();
            availableCoordinates.removeAll(bodyCoordinates);
        }

        var foodCoordinates = gameState.getFoodCoordinates();
        availableCoordinates.removeAll(foodCoordinates);

        return availableCoordinates;
    }
}
