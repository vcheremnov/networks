package gamemodel.model;

import gametree.NodeRole;
import gamemodel.model.auxiliary.PlayerType;

public class GamePlayer {
    private static final int INITIAL_SCORE = 0;
    private static final String DEFAULT_NAME = "Player";

    private PlayerType playerType = PlayerType.HUMAN;
    private int score = INITIAL_SCORE;
    private String name = DEFAULT_NAME;
    private Integer playerID;

    private NodeRole nodeRole = NodeRole.MASTER;
    private String address;
    private Integer port;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPlayerID() {
        return playerID;
    }

    public void setPlayerID(int playerID) {
        this.playerID = playerID;
    }

    public NodeRole getNodeRole() {
        return nodeRole;
    }

    public void setNodeRole(NodeRole nodeRole) {
        this.nodeRole = nodeRole;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public void setPlayerType(PlayerType playerType) {
        this.playerType = playerType;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
