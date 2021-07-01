package gametree.communication.messages;

import gamemodel.model.GameState;
import gamemodel.model.auxiliary.Direction;
import gamemodel.model.auxiliary.PlayerType;
import gametree.NodeRole;
import gametree.communication.GameMessage;

import java.util.concurrent.atomic.AtomicLong;

public class GameMessageFactory {
    private static AtomicLong messageID = new AtomicLong(0);

    private static void fillBaseFields(GameMessage message, Integer senderID, Integer receiverID) {
        fillBaseFields(message, messageID.getAndIncrement(), senderID, receiverID);
    }

    private static void fillBaseFields(GameMessage message, long messageID,
                                       Integer senderID, Integer receiverID) {
        message.setSenderID(senderID);
        message.setReceiverID(receiverID);
        message.setMessageID(messageID);
    }

    public static GameMessage createAckMessage(long messageID, Integer senderID, Integer receiverID) {
        AckGameMessage message = new AckGameMessage();
        fillBaseFields(message, messageID, senderID, receiverID);
        return message;
    }

    public static GameMessage createErrorMessage(long messageID, Integer senderID,
                                                 Integer receiverID, String errorDescription) {
        ErrorGameMessage message = new ErrorGameMessage();
        fillBaseFields(message, messageID, senderID, receiverID);
        message.setErrorDescription(errorDescription);
        return message;
    }

    public static GameMessage createPingMessage(Integer senderID, Integer receiverID) {
        PingGameMessage message = new PingGameMessage();
        fillBaseFields(message, senderID, receiverID);
        return message;
    }

    public static GameMessage createRoleChangeMessage(Integer senderID, Integer receiverID,
                                                      NodeRole senderRole, NodeRole receiverRole) {
        RoleChangeGameMessage message = new RoleChangeGameMessage();
        fillBaseFields(message, senderID, receiverID);
        message.setSenderRole(senderRole);
        message.setReceiverRole(receiverRole);
        return message;
    }

    public static GameMessage createSteerMessage(Integer senderID, Integer receiverID, Direction direction) {
        SteerGameMessage message = new SteerGameMessage();
        fillBaseFields(message, senderID, receiverID);
        message.setDirection(direction);
        return message;
    }

    public static GameMessage createStateMessage(Integer senderID, Integer receiverID, GameState gameState) {
        StateGameMessage message = new StateGameMessage();
        fillBaseFields(message, senderID, receiverID);
        message.setGameState(gameState);
        return message;
    }

    public static GameMessage createAnnouncementMessage(Integer senderID, GameState gameState) {
        AnnouncementGameMessage message = new AnnouncementGameMessage();
        fillBaseFields(message, senderID, null);
        message.setPlayers(gameState.getPlayers().values());
        message.setGameConfig(gameState.getGameConfig());
        message.setCanJoinFlag(true);
        return message;
    }

    public static GameMessage createJoinMessage(Integer receiverID, String playerName, boolean onlyView) {
        JoinGameMessage message = new JoinGameMessage();
        fillBaseFields(message, null, receiverID);
        message.setPlayerName(playerName);
        message.setPlayerType(PlayerType.HUMAN);
        message.setOnlyViewFlag(onlyView);
        return message;
    }
}
