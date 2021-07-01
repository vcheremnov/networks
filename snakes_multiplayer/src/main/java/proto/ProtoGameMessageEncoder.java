package proto;

import gamemodel.model.GameConfig;
import gamemodel.model.GamePlayer;
import gamemodel.model.GameState;
import gamemodel.model.Snake;
import gamemodel.model.auxiliary.Coordinate;
import gamemodel.model.auxiliary.Direction;
import gamemodel.model.auxiliary.PlayerType;
import gametree.NodeRole;
import gametree.communication.GameMessage;
import gametree.communication.GameMessageEncoder;
import gametree.communication.messages.*;
import network.messages.auxiliary.Message;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ProtoGameMessageEncoder implements GameMessageEncoder {
    @Override
    public Message encodeGameMessage(GameMessage gameMessage) {
        Message message = new Message();
        message.setSenderID(gameMessage.getSenderID());
        message.setReceiverID(gameMessage.getReceiverID());
        message.setMessageID(gameMessage.getMessageID());

        SnakesProto.GameMessage protoGameMessage = null;
        switch (gameMessage.getMessageType()) {
            case ACK_MESSAGE:
                protoGameMessage = encodeAckMessage((AckGameMessage) gameMessage);
                break;
            case JOIN_MESSAGE:
                protoGameMessage = encodeJoinMessage((JoinGameMessage) gameMessage);
                break;
            case PING_MESSAGE:
                protoGameMessage = encodePingMessage((PingGameMessage) gameMessage);
                break;
            case ERROR_MESSAGE:
                protoGameMessage = encodeErrorMessage((ErrorGameMessage) gameMessage);
                break;
            case STATE_MESSAGE:
                protoGameMessage = encodeStateMessage((StateGameMessage) gameMessage);
                break;
            case STEER_MESSAGE:
                protoGameMessage = encodeSteerMessage((SteerGameMessage) gameMessage);
                break;
            case ROLE_CHANGE_MESSAGE:
                protoGameMessage = encodeRoleChangeMessage((RoleChangeGameMessage) gameMessage);
                break;
            case ANNOUNCEMENT_MESSAGE:
                protoGameMessage = encodeAnnouncementMessage((AnnouncementGameMessage) gameMessage);
                break;
        }

        message.setMessageBytes(protoGameMessage.toByteArray());
        return message;
    }

    private SnakesProto.GameMessage.Builder getGameMessageBuilder(GameMessage gameMessage) {
        return SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(gameMessage.getMessageID())
                .setSenderId(gameMessage.getSenderID() == null ? -1 : gameMessage.getSenderID())
                .setReceiverId(gameMessage.getReceiverID() == null ? -1 : gameMessage.getReceiverID());
    }

    private SnakesProto.NodeRole getProtoNodeRole(NodeRole nodeRole) {
        switch (nodeRole) {
            case MASTER: return SnakesProto.NodeRole.MASTER;
            case DEPUTY: return SnakesProto.NodeRole.DEPUTY;
            case VIEWER: return SnakesProto.NodeRole.VIEWER;
            case NORMAL: return SnakesProto.NodeRole.NORMAL;
            default:     return null;
        }
    }

    private SnakesProto.PlayerType getProtoPlayerType(PlayerType playerType) {
        switch (playerType) {
            case HUMAN: return SnakesProto.PlayerType.HUMAN;
            case ROBOT: return SnakesProto.PlayerType.ROBOT;
            default:    return null;
        }
    }

    private SnakesProto.Direction getProtoDirection(Direction direction) {
        switch (direction) {
            case LEFT:  return SnakesProto.Direction.LEFT;
            case UP:    return SnakesProto.Direction.UP;
            case DOWN:  return SnakesProto.Direction.DOWN;
            case RIGHT: return SnakesProto.Direction.RIGHT;
            default:    return null;
        }
    }

    private SnakesProto.GamePlayer getProtoGamePlayer(GamePlayer gamePlayer) {
        return SnakesProto.GamePlayer.newBuilder()
                .setId(gamePlayer.getPlayerID())
                .setName(gamePlayer.getName())
                .setIpAddress(gamePlayer.getAddress())
                .setPort(gamePlayer.getPort())
                .setRole(getProtoNodeRole(gamePlayer.getNodeRole()))
                .setType(getProtoPlayerType(gamePlayer.getPlayerType()))
                .setScore(gamePlayer.getScore())
                .build();
    }

    private SnakesProto.GamePlayers getProtoGamePlayers(Collection<GamePlayer> gamePlayers) {
        Collection<SnakesProto.GamePlayer> protoGamePlayers =
                gamePlayers.stream()
                .map(this::getProtoGamePlayer)
                .collect(Collectors.toList());

        return SnakesProto.GamePlayers.newBuilder()
                .addAllPlayers(protoGamePlayers)
                .build();
    }

    private SnakesProto.GameConfig getProtoGameConfig(GameConfig gameConfig) {
        return SnakesProto.GameConfig.newBuilder()
                .setDeadFoodProb(gameConfig.getDeadFoodProbability())
                .setFoodPerPlayer(gameConfig.getFoodPerPlayer())
                .setFoodStatic(gameConfig.getStaticFoodAmount())
                .setNodeTimeoutMs(gameConfig.getNodeTimeoutMillis())
                .setHeight(gameConfig.getFieldSize().getHeight())
                .setWidth(gameConfig.getFieldSize().getWidth())
                .setPingDelayMs(gameConfig.getPingDelayMillis())
                .setStateDelayMs(gameConfig.getStateDelayMillis())
                .build();
    }

    private SnakesProto.GameState.Coord getProtoCoord(Coordinate coordinate) {
        return SnakesProto.GameState.Coord.newBuilder()
                .setX(coordinate.getX())
                .setY(coordinate.getY())
                .build();
    }

    // TODO: конвертация тела змейки в формат, указанный в протоколе
    private List<SnakesProto.GameState.Coord> convertSnakeBody(LinkedList<Coordinate> snakeBody) {
        return snakeBody.stream().map(this::getProtoCoord).collect(Collectors.toList());
    }

    private SnakesProto.GameState.Snake getProtoSnake(Snake snake, int playerID) {
        return SnakesProto.GameState.Snake.newBuilder()
                .setState(
                        snake.isZombie() ?
                        SnakesProto.GameState.Snake.SnakeState.ZOMBIE :
                        SnakesProto.GameState.Snake.SnakeState.ALIVE
                ).setPlayerId(playerID)
                .addAllPoints(convertSnakeBody(snake.getBodyCoordinates()))
                .setHeadDirection(getProtoDirection(snake.getDirection()))
                .build();
    }

    private SnakesProto.GameState getProtoGameState(GameState gameState) {
        var idToSnakeMap = gameState.getSnakes();
        Collection<SnakesProto.GameState.Snake> protoSnakes =
                idToSnakeMap.keySet().stream()
                .map(snakeID -> getProtoSnake(idToSnakeMap.get(snakeID), snakeID))
                .collect(Collectors.toList());

        Collection<SnakesProto.GameState.Coord> protoFoodCoords =
                gameState.getFoodCoordinates().stream()
                .map(this::getProtoCoord)
                .collect(Collectors.toList());

        return SnakesProto.GameState.newBuilder()
                .setStateOrder(gameState.getStateOrder())
                .addAllFoods(protoFoodCoords)
                .addAllSnakes(protoSnakes)
                .setPlayers(getProtoGamePlayers(gameState.getPlayers().values()))
                .setConfig(getProtoGameConfig(gameState.getGameConfig()))
                .build();
    }

    private SnakesProto.GameMessage encodeAnnouncementMessage(AnnouncementGameMessage gameMessage) {
        SnakesProto.GameMessage.AnnouncementMsg announcementMsg =
                SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                .setCanJoin(gameMessage.canJoin())
                .setConfig(getProtoGameConfig(gameMessage.getGameConfig()))
                .setPlayers(getProtoGamePlayers(gameMessage.getPlayers()))
                .build();

        return getGameMessageBuilder(gameMessage)
                .setAnnouncement(announcementMsg)
                .build();
    }

    private SnakesProto.GameMessage encodeRoleChangeMessage(RoleChangeGameMessage gameMessage) {
        NodeRole senderRole = (gameMessage.getSenderRole() == null) ?
                NodeRole.NORMAL : gameMessage.getSenderRole();
        NodeRole receiverRole = (gameMessage.getReceiverRole() == null) ?
                NodeRole.NORMAL : gameMessage.getReceiverRole();

        SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg =
                SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                .setSenderRole(getProtoNodeRole(senderRole))
                .setReceiverRole(getProtoNodeRole(receiverRole))
                .build();

        return getGameMessageBuilder(gameMessage)
                .setRoleChange(roleChangeMsg)
                .build();
    }

    private SnakesProto.GameMessage encodeSteerMessage(SteerGameMessage gameMessage) {
        SnakesProto.GameMessage.SteerMsg steerMsg =
                SnakesProto.GameMessage.SteerMsg.newBuilder()
                .setDirection(getProtoDirection(gameMessage.getDirection()))
                .build();

        return getGameMessageBuilder(gameMessage)
                .setSteer(steerMsg)
                .build();
    }

    private SnakesProto.GameMessage encodeStateMessage(StateGameMessage gameMessage) {
        SnakesProto.GameMessage.StateMsg stateMsg =
                SnakesProto.GameMessage.StateMsg.newBuilder()
                .setState(getProtoGameState(gameMessage.getGameState()))
                .build();

        return getGameMessageBuilder(gameMessage)
                .setState(stateMsg)
                .build();
    }

    private SnakesProto.GameMessage encodeErrorMessage(ErrorGameMessage gameMessage) {
        SnakesProto.GameMessage.ErrorMsg errorMsg =
                SnakesProto.GameMessage.ErrorMsg.newBuilder()
                .setErrorMessage(gameMessage.getErrorDescription())
                .build();

        return getGameMessageBuilder(gameMessage)
                .setError(errorMsg)
                .build();
    }

    private SnakesProto.GameMessage encodePingMessage(PingGameMessage gameMessage) {
        SnakesProto.GameMessage.PingMsg pingMsg =
                SnakesProto.GameMessage.PingMsg.newBuilder()
                .build();

        return getGameMessageBuilder(gameMessage)
                .setPing(pingMsg)
                .build();
    }

    private SnakesProto.GameMessage encodeJoinMessage(JoinGameMessage gameMessage) {
        SnakesProto.GameMessage.JoinMsg joinMsg =
                SnakesProto.GameMessage.JoinMsg.newBuilder()
                .setName(gameMessage.getPlayerName())
                .setPlayerType(getProtoPlayerType(gameMessage.getPlayerType()))
                .setOnlyView(gameMessage.isOnlyView())
                .build();

        return getGameMessageBuilder(gameMessage)
                .setJoin(joinMsg)
                .build();
    }

    private SnakesProto.GameMessage encodeAckMessage(AckGameMessage gameMessage) {
        SnakesProto.GameMessage.AckMsg ackMsg =
                SnakesProto.GameMessage.AckMsg.newBuilder()
                .build();

        return getGameMessageBuilder(gameMessage)
                .setAck(ackMsg)
                .build();
    }
}
