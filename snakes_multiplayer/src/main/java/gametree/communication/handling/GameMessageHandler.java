package gametree.communication.handling;

import gamemodel.model.GameConfig;
import gamemodel.model.GamePlayer;
import gamemodel.model.GameState;
import gamemodel.model.auxiliary.FieldSize;
import gamemodel.model.auxiliary.PlayerAdditionResult;
import gametree.GameTreeNodeData;
import gametree.NodeRole;
import gametree.ServerInfo;
import gametree.communication.GameMessage;
import gametree.communication.GameMessageDecoder;
import gametree.communication.GameMessageSender;
import gametree.communication.GameMessageType;
import gametree.communication.messages.*;
import network.messages.MessageHandler;
import network.messages.auxiliary.HandledMessage;
import network.messages.auxiliary.Message;
import network.messages.auxiliary.ReceivedMessage;
import network.suppliers.ReceivedMessageSupplier;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameMessageHandler extends MessageHandler {
    private GameMessageDecoder gameMessageDecoder;
    private final GameTreeNodeData nodeData;
    private GameMessageSender gameMessageSender;
    private NodeRoleManager nodeRoleManager;

    private final Map<Integer, Long> playerIdToLastSteerMsgID = new ConcurrentHashMap<>();
    private final Map<InetSocketAddress, Integer> socketAddressToJoinedPlayerID = new ConcurrentHashMap<>();
    private final Set<InetSocketAddress> allowedAddresses = ConcurrentHashMap.newKeySet();

    public GameMessageHandler(ReceivedMessageSupplier receivedMessageSupplier,
                              GameMessageDecoder gameMessageDecoder,
                              GameMessageSender gameMessageSender,
                              NodeRoleManager nodeRoleManager,
                              GameTreeNodeData nodeData) {
        super(receivedMessageSupplier);
        this.nodeData = nodeData;
        this.nodeRoleManager = nodeRoleManager;
        this.gameMessageSender = gameMessageSender;
        this.gameMessageDecoder = gameMessageDecoder;
    }

    public void clearData() {
        playerIdToLastSteerMsgID.clear();
        socketAddressToJoinedPlayerID.clear();
        allowedAddresses.clear();
    }

    public void addAllowedAddress(InetSocketAddress inetSocketAddress) {
        allowedAddresses.add(inetSocketAddress);
    }

    public void removeAllowedAddress(InetSocketAddress inetSocketAddress) {
        allowedAddresses.remove(inetSocketAddress);
    }

    @Override
    public HandledMessage handleMessage(ReceivedMessage receivedMessage) throws Exception {
        byte[] messageBytes = receivedMessage.getMessageBytes();
        InetSocketAddress senderSocketAddress = receivedMessage.getSenderSocketAddress();

        GameMessage gameMessage = gameMessageDecoder.decodeGameMessage(messageBytes);
        GameMessageType messageType = gameMessage.getMessageType();

        if (messageType != GameMessageType.ANNOUNCEMENT_MESSAGE &&
            messageType != GameMessageType.JOIN_MESSAGE &&
            !allowedAddresses.contains(senderSocketAddress)) {
            return null;
        }

        Message message = new Message();
        message.setMessageBytes(messageBytes);
        message.setMessageID(gameMessage.getMessageID());
        message.setReceiverID(gameMessage.getReceiverID());
        message.setSenderID(gameMessage.getSenderID());

        HandledMessage handledMessage = new HandledMessage();
        handledMessage.setReceiveTimestamp(receivedMessage.getTimestamp());
        handledMessage.setMessage(message);
        handledMessage.setAckMessageFlag(
                messageType == GameMessageType.ACK_MESSAGE ||
                messageType == GameMessageType.ERROR_MESSAGE
        );

        GameMessage responseMessage = null;
        switch (messageType) {
            case ACK_MESSAGE:
                responseMessage = handleAckMessage((AckGameMessage) gameMessage);
                break;
            case JOIN_MESSAGE:
                responseMessage = handleJoinMessage(
                        (JoinGameMessage) gameMessage,
                        senderSocketAddress
                );
                break;
            case PING_MESSAGE:
                responseMessage = handlePingMessage((PingGameMessage) gameMessage);
                break;
            case ERROR_MESSAGE:
                responseMessage = handleErrorMessage((ErrorGameMessage) gameMessage);
                break;
            case STATE_MESSAGE:
                responseMessage = handleStateMessage(
                        (StateGameMessage) gameMessage,
                        senderSocketAddress
                );
                break;
            case STEER_MESSAGE:
                responseMessage = handleSteerMessage((SteerGameMessage) gameMessage);
                break;
            case ROLE_CHANGE_MESSAGE:
                responseMessage = handleRoleChangeMessage((RoleChangeGameMessage) gameMessage);
                break;
            case ANNOUNCEMENT_MESSAGE:
                responseMessage = handleAnnouncementMessage(
                        (AnnouncementGameMessage) gameMessage,
                        senderSocketAddress
                );
                break;
        }

        if (responseMessage != null) {
            gameMessageSender.sendGameMessage(responseMessage, senderSocketAddress);
        }

        return handledMessage;
    }

    private GameMessage handleAnnouncementMessage(AnnouncementGameMessage message,
                                                  InetSocketAddress senderSocketAddress) {
        Collection<GamePlayer> players = message.getPlayers();
        GameConfig gameConfig = message.getGameConfig();

        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setInetSocketAddress(senderSocketAddress);
        serverInfo.setGameConfig(gameConfig);
        serverInfo.setMasterID(message.getSenderID());

        FieldSize fieldSize = gameConfig.getFieldSize();
        serverInfo.setFieldSize(
                String.format("%d x %d", fieldSize.getWidth(), fieldSize.getHeight())
        );

        long playersNumber = players.stream()
                .filter(player -> player.getNodeRole() != NodeRole.VIEWER)
                .count();
        serverInfo.setPlayersNumber((int) playersNumber);

        String masterName = players.stream()
                .filter(player -> player.getNodeRole() == NodeRole.MASTER)
                .findFirst()
                .map(GamePlayer::getName)
                .orElse("...");
        serverInfo.setMasterName(masterName);

        Integer deputyID = players.stream()
                .filter(player -> player.getNodeRole() == NodeRole.DEPUTY)
                .findFirst()
                .map(GamePlayer::getPlayerID)
                .orElse(null);
        serverInfo.setDeputyID(deputyID);

        serverInfo.setFoodFormula(
                String.format(
                        "%d + %.2f * x",
                        gameConfig.getStaticFoodAmount(),
                        gameConfig.getFoodPerPlayer()
                )
        );

        nodeData.addServerInfo(serverInfo);

        return null;
    }

    private GameMessage handleErrorMessage(ErrorGameMessage message) {
        String errorDescription = message.getErrorDescription();
        nodeData.addInfoMessage(errorDescription);
        return null;
    }

    private GameMessage handleAckMessage(AckGameMessage message) {
        if (message.getReceiverID() != null) {
            nodeData.setPlayerID(message.getReceiverID());
//            nodeData.assignRoleToPlayer(NodeRole.NORMAL, message.getReceiverID());
            nodeData.startGame();
        }

        return null;
    }

    private GameMessage handleJoinMessage(JoinGameMessage message, InetSocketAddress senderSocketAddress) throws IOException {
        if (socketAddressToJoinedPlayerID.containsKey(senderSocketAddress)) {
            Integer joinedPlayerID = socketAddressToJoinedPlayerID.remove(senderSocketAddress);
            synchronized (nodeData) {
                nodeData.removePlayer(joinedPlayerID);
                if (joinedPlayerID.equals(nodeData.getDeputyPlayerID())) {
                    nodeData.assignRoleToPlayer(NodeRole.DEPUTY, null);
                }
            }
        }

        GamePlayer player = new GamePlayer();
        player.setNodeRole(message.isOnlyView() ? NodeRole.VIEWER : NodeRole.NORMAL);
        player.setName(message.getPlayerName());
        player.setAddress(senderSocketAddress.getAddress().getHostAddress());
        player.setPort(senderSocketAddress.getPort());

        PlayerAdditionResult result = nodeData.addPlayer(player);
        Integer joinedPlayerID = player.getPlayerID();

        switch (result) {
            case SUCCESS:
                GameMessage ackMessage = GameMessageFactory.createAckMessage(
                        message.getMessageID(),
                        nodeData.getPlayerID(),
                        joinedPlayerID
                );
                gameMessageSender.sendGameMessage(ackMessage, senderSocketAddress);

                allowedAddresses.add(senderSocketAddress);
                socketAddressToJoinedPlayerID.put(senderSocketAddress, joinedPlayerID);
                if (nodeData.getDeputyPlayerID() == null) {
                    Integer deputyID = nodeRoleManager.findNewDeputyPlayer();
                    GameMessage roleChangeMessage = GameMessageFactory.createRoleChangeMessage(
                            nodeData.getPlayerID(), deputyID, null, NodeRole.DEPUTY
                    );
                    gameMessageSender.sendGameMessage(roleChangeMessage, senderSocketAddress);
                }

                return null;
            case NAME_EXISTS_ERROR:
                return GameMessageFactory.createErrorMessage(
                        message.getMessageID(),
                        nodeData.getPlayerID(),
                        joinedPlayerID,
                        String.format("Name \"%s\" is already used", player.getName())
                );
            case NO_PLACE_ERROR:
                return GameMessageFactory.createErrorMessage(
                        message.getMessageID(),
                        nodeData.getPlayerID(),
                        joinedPlayerID,
                        "No place on the field, try again later"
                );
            case ID_EXISTS_ERROR:
                throw new RuntimeException("Tried to add player with already existing ID");
            case NOT_MASTER_ERROR:
                throw new RuntimeException("Tried to add a player, though Not a master node");
            default:
                throw new RuntimeException("Not implemented yet");
        }
    }

    private GameMessage handlePingMessage(PingGameMessage message) {
        return getAckMessage(message);
    }

    private GameMessage handleStateMessage(StateGameMessage message,
                                           InetSocketAddress senderSocketAddress) {
        GameState gameState = message.getGameState();
        boolean stateWasSet = nodeData.setGameState(gameState);

        if (stateWasSet) {
            for (var player: gameState.getPlayers().values()) {
                if (message.getSenderID().equals(player.getPlayerID())) {
                    player.setAddress(senderSocketAddress.getAddress().getHostAddress());
                    player.setPort(senderSocketAddress.getPort());
                } else if (player.getNodeRole() == NodeRole.DEPUTY) {
                    nodeData.assignRoleToPlayer(NodeRole.DEPUTY, player.getPlayerID());
                }
            }
        }

        return getAckMessage(message);
    }

    private GameMessage handleSteerMessage(SteerGameMessage message) {
        Integer senderID = message.getSenderID();
        Long messageID = message.getMessageID();

        Long lastSteerMessageID = playerIdToLastSteerMsgID.get(senderID);
        if (lastSteerMessageID == null || messageID > lastSteerMessageID) {
            nodeData.setPlayerDirection(senderID, message.getDirection());
            playerIdToLastSteerMsgID.put(senderID, messageID);
        }

        return getAckMessage(message);
    }

    private GameMessage handleRoleChangeMessage(RoleChangeGameMessage message) throws IOException {
        nodeRoleManager.handleNodeRoleChange(
                message.getSenderID(), message.getReceiverID(),
                message.getSenderRole(), message.getReceiverRole()
        );
        return getAckMessage(message);
    }

    // TODO: receiverID - null! ничего ж не сломается, правда?
    private GameMessage getAckMessage(GameMessage message) {
        return GameMessageFactory.createAckMessage(
                    message.getMessageID(),
                    message.getReceiverID(),
                    null
        );
    }
}