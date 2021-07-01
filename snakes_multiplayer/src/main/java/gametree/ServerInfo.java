package gametree;

import gamemodel.model.GameConfig;
import proto.SnakesProto;

import java.net.InetSocketAddress;

public class ServerInfo {
    private Integer masterID;
    private Integer deputyID;
    private String masterName;
    private Integer playersNumber;
    private String fieldSize;
    private String foodFormula;
    private InetSocketAddress inetSocketAddress;
    private GameConfig gameConfig;

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    public Integer getPlayersNumber() {
        return playersNumber;
    }

    public void setPlayersNumber(Integer playersNumber) {
        this.playersNumber = playersNumber;
    }

    public String getFieldSize() {
        return fieldSize;
    }

    public void setFieldSize(String fieldSize) {
        this.fieldSize = fieldSize;
    }

    public String getFoodFormula() {
        return foodFormula;
    }

    public void setFoodFormula(String foodFormula) {
        this.foodFormula = foodFormula;
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }

    public void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
        this.inetSocketAddress = inetSocketAddress;
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
        ServerInfo serverInfo = (ServerInfo) o;
        // field comparison
        return inetSocketAddress.equals(serverInfo.inetSocketAddress);
    }

    @Override
    public int hashCode() {
        return inetSocketAddress.hashCode();
    }

    public Integer getMasterID() {
        return masterID;
    }

    public void setMasterID(Integer masterID) {
        this.masterID = masterID;
    }

    public Integer getDeputyID() {
        return deputyID;
    }

    public void setDeputyID(Integer deputyID) {
        this.deputyID = deputyID;
    }

    public GameConfig getGameConfig() {
        return gameConfig;
    }

    public void setGameConfig(GameConfig gameConfig) {
        this.gameConfig = gameConfig;
    }
}
