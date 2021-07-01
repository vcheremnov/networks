package proto;

import com.google.protobuf.InvalidProtocolBufferException;
import gamemodel.model.GameConfig;
import gamemodel.model.GamePlayer;
import gamemodel.model.GameState;
import gamemodel.model.Snake;
import gamemodel.model.auxiliary.Coordinate;
import gamemodel.model.auxiliary.Direction;
import gamemodel.model.auxiliary.FieldSize;
import gamemodel.model.auxiliary.PlayerType;
import gametree.NodeRole;
import gametree.communication.GameMessage;
import gametree.communication.GameMessageDecoder;
import gametree.communication.messages.*;

import java.util.*;
import java.util.stream.Collectors;

public class ProtoGameMessageDecoder implements GameMessageDecoder {
    @Override
    public GameMessage decodeGameMessage(byte[] messageBytes) throws InvalidProtocolBufferException {
        GameMessage gameMessage;
        SnakesProto.GameMessage protoGameMessage = SnakesProto.GameMessage.parseFrom(messageBytes);
        switch (protoGameMessage.getTypeCase()) {
            case ACK:
                gameMessage = decodeAckMessage(protoGameMessage.getAck());
                break;
            case JOIN:
                gameMessage = decodeJoinMessage(protoGameMessage.getJoin());
                break;
            case PING:
                gameMessage = decodePingMessage(protoGameMessage.getPing());
                break;
            case ERROR:
                gameMessage = decodeErrorMessage(protoGameMessage.getError());
                break;
            case STATE:
                gameMessage = decodeStateMessage(protoGameMessage.getState());
                break;
            case STEER:
                gameMessage = decodeSteerMessage(protoGameMessage.getSteer());
                break;
            case ROLE_CHANGE:
                gameMessage = decodeRoleChangeMessage(protoGameMessage.getRoleChange());
                break;
            case ANNOUNCEMENT:
                gameMessage = decodeAnnouncementMessage(protoGameMessage.getAnnouncement());
                break;
            default:
                throw new RuntimeException("Message type was not set");
        }

        gameMessage.setMessageID(protoGameMessage.getMsgSeq());
        gameMessage.setSenderID(
                protoGameMessage.getSenderId() == -1 ? null : protoGameMessage.getSenderId()
        );
        gameMessage.setReceiverID(
                protoGameMessage.getReceiverId() == -1 ? null : protoGameMessage.getReceiverId()
        );

        return gameMessage;
    }

    private Direction getDirection(SnakesProto.Direction protoDirection) {
        switch (protoDirection) {
            case UP:    return Direction.UP;
            case DOWN:  return Direction.DOWN;
            case LEFT:  return Direction.LEFT;
            case RIGHT: return Direction.RIGHT;
            default:    return null;
        }
    }

    private NodeRole getNodeRole(SnakesProto.NodeRole protoNodeRole) {
        if (protoNodeRole == null) {
            return null;
        }

        switch (protoNodeRole) {
            case MASTER: return NodeRole.MASTER;
            case DEPUTY: return NodeRole.DEPUTY;
            case VIEWER: return NodeRole.VIEWER;
            case NORMAL: return NodeRole.NORMAL;
            default:     return null;
        }
    }

    private PlayerType getPlayerType(SnakesProto.PlayerType protoPlayerType) {
        switch (protoPlayerType) {
            case HUMAN: return PlayerType.HUMAN;
            case ROBOT: return PlayerType.ROBOT;
            default:    return null;
        }
    }

    private Coordinate getCoordinate(SnakesProto.GameState.Coord protoCoord) {
        return new Coordinate(protoCoord.getX(), protoCoord.getY());
    }

    // TODO: перевод тела змеи из формата, указанного в протоколе, в полный список всех клеток
    private LinkedList<Coordinate> convertSnakeBody(List<SnakesProto.GameState.Coord> protoSnakeBody) {
        return protoSnakeBody.stream()
                .map(this::getCoordinate)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private GameConfig getGameConfig(SnakesProto.GameConfig protoGameConfig) {
        GameConfig gameConfig = new GameConfig();
        gameConfig.setDeadFoodProbability(protoGameConfig.getDeadFoodProb());
        gameConfig.setStaticFoodAmount(protoGameConfig.getFoodStatic());
        gameConfig.setFoodPerPlayer(protoGameConfig.getFoodPerPlayer());
        gameConfig.setNodeTimeoutMillis(protoGameConfig.getNodeTimeoutMs());
        gameConfig.setPingDelayMillis(protoGameConfig.getPingDelayMs());
        gameConfig.setStateDelayMillis(protoGameConfig.getStateDelayMs());
        gameConfig.setFieldSize(new FieldSize(protoGameConfig.getWidth(), protoGameConfig.getHeight()));

        return gameConfig;
    }

    private GameState getGameState(SnakesProto.GameState protoGameState) {
        GameState gameState = new GameState(getGameConfig(protoGameState.getConfig()));
        gameState.setStateOrder(protoGameState.getStateOrder());

        List<Coordinate> foodCoordinates = protoGameState.getFoodsList().stream()
                .map(this::getCoordinate)
                .collect(Collectors.toList());
        gameState.getFoodCoordinates().addAll(foodCoordinates);

        Map<Integer, GamePlayer> gamePlayersMap = protoGameState.getPlayers().getPlayersList().stream()
                .collect(
                    Collectors.toMap(
                        SnakesProto.GamePlayer::getId,
                        this::getGamePlayer
                    )
                );
        gameState.getPlayers().putAll(gamePlayersMap);

        Map<Integer, Snake> snakesMap = protoGameState.getSnakesList().stream()
                .collect(
                        Collectors.toMap(
                                SnakesProto.GameState.Snake::getPlayerId,
                                this::getSnake
                        )
                );
        gameState.getSnakes().putAll(snakesMap);

        return gameState;
    }

    private GamePlayer getGamePlayer(SnakesProto.GamePlayer protoGamePlayer) {
        GamePlayer gamePlayer = new GamePlayer();
        gamePlayer.setName(protoGamePlayer.getName());
        gamePlayer.setPlayerID(protoGamePlayer.getId());
        gamePlayer.setAddress(protoGamePlayer.getIpAddress());
        gamePlayer.setPort(protoGamePlayer.getPort());
        gamePlayer.setNodeRole(getNodeRole(protoGamePlayer.getRole()));
        gamePlayer.setPlayerType(getPlayerType(protoGamePlayer.getType()));
        gamePlayer.setScore(protoGamePlayer.getScore());

        return gamePlayer;
    }

    private Collection<GamePlayer> getGamePlayers(SnakesProto.GamePlayers protoGamePlayers) {
        return protoGamePlayers.getPlayersList().stream()
                .map(this::getGamePlayer)
                .collect(Collectors.toList());
    }

    private Snake getSnake(SnakesProto.GameState.Snake protoSnake) {
        Snake snake = new Snake(
            convertSnakeBody(protoSnake.getPointsList()),
            getDirection(protoSnake.getHeadDirection())
        );

        if (protoSnake.getState() == SnakesProto.GameState.Snake.SnakeState.ZOMBIE) {
            snake.makeZombie();
        }

        return snake;
    }

    private GameMessage decodeAnnouncementMessage(SnakesProto.GameMessage.AnnouncementMsg announcement) {
        AnnouncementGameMessage announcementGameMessage = new AnnouncementGameMessage();
        announcementGameMessage.setCanJoinFlag(announcement.getCanJoin());
        announcementGameMessage.setGameConfig(getGameConfig(announcement.getConfig()));
        announcementGameMessage.setPlayers(getGamePlayers(announcement.getPlayers()));
        return announcementGameMessage;
    }

    private GameMessage decodeStateMessage(SnakesProto.GameMessage.StateMsg state) {
        StateGameMessage stateGameMessage = new StateGameMessage();
        stateGameMessage.setGameState(getGameState(state.getState()));
        return stateGameMessage;
    }

    private GameMessage decodeJoinMessage(SnakesProto.GameMessage.JoinMsg join) {
        JoinGameMessage joinGameMessage = new JoinGameMessage();
        joinGameMessage.setOnlyViewFlag(join.getOnlyView());
        joinGameMessage.setPlayerName(join.getName());
        joinGameMessage.setPlayerType(getPlayerType(join.getPlayerType()));
        return joinGameMessage;
    }

    private GameMessage decodeRoleChangeMessage(SnakesProto.GameMessage.RoleChangeMsg roleChange) {
        RoleChangeGameMessage roleChangeGameMessage = new RoleChangeGameMessage();

        SnakesProto.NodeRole senderRole = (roleChange.getSenderRole() == SnakesProto.NodeRole.NORMAL) ?
                null : roleChange.getSenderRole();
        SnakesProto.NodeRole receiverRole = (roleChange.getReceiverRole() == SnakesProto.NodeRole.NORMAL) ?
                null : roleChange.getReceiverRole();

        roleChangeGameMessage.setReceiverRole(getNodeRole(receiverRole));
        roleChangeGameMessage.setSenderRole(getNodeRole(senderRole));
        return roleChangeGameMessage;
    }

    private GameMessage decodeSteerMessage(SnakesProto.GameMessage.SteerMsg steer) {
        SteerGameMessage steerGameMessage = new SteerGameMessage();
        steerGameMessage.setDirection(getDirection(steer.getDirection()));
        return steerGameMessage;
    }

    private GameMessage decodeErrorMessage(SnakesProto.GameMessage.ErrorMsg error) {
        ErrorGameMessage errorGameMessage = new ErrorGameMessage();
        errorGameMessage.setErrorDescription(error.getErrorMessage());
        return errorGameMessage;
    }

    private GameMessage decodePingMessage(SnakesProto.GameMessage.PingMsg ping) {
        return new PingGameMessage();
    }

    private GameMessage decodeAckMessage(SnakesProto.GameMessage.AckMsg ack) {
        return new AckGameMessage();
    }
}
