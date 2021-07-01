package gamemodel.model;

import gamemodel.model.auxiliary.FieldSize;

public class GameConfig {
    private FieldSize fieldSize;

    private float deadFoodProbability;
    private int staticFoodAmount;
    private float foodPerPlayer;

    private int stateDelayMillis;
    private int pingDelayMillis;
    private int nodeTimeoutMillis;

    public float getDeadFoodProbability() {
        return deadFoodProbability;
    }

    public void setDeadFoodProbability(float deadFoodProbability) {
        this.deadFoodProbability = deadFoodProbability;
    }

    public int getStaticFoodAmount() {
        return staticFoodAmount;
    }

    public void setStaticFoodAmount(int staticFoodAmount) {
        this.staticFoodAmount = staticFoodAmount;
    }

    public float getFoodPerPlayer() {
        return foodPerPlayer;
    }

    public void setFoodPerPlayer(float foodPerPlayer) {
        this.foodPerPlayer = foodPerPlayer;
    }

    public int getStateDelayMillis() {
        return stateDelayMillis;
    }

    public void setStateDelayMillis(int stateDelayMillis) {
        this.stateDelayMillis = stateDelayMillis;
    }

    public int getPingDelayMillis() {
        return pingDelayMillis;
    }

    public void setPingDelayMillis(int pingDelayMillis) {
        this.pingDelayMillis = pingDelayMillis;
    }

    public int getNodeTimeoutMillis() {
        return nodeTimeoutMillis;
    }

    public void setNodeTimeoutMillis(int nodeTimeoutMillis) {
        this.nodeTimeoutMillis = nodeTimeoutMillis;
    }

    public FieldSize getFieldSize() {
        return fieldSize;
    }

    public void setFieldSize(FieldSize fieldSize) {
        this.fieldSize = fieldSize;
    }
}
