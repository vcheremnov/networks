package gametree;

import gamemodel.model.GameConfig;
import gamemodel.model.GamePlayer;
import gamemodel.logic.GameStateController;
import gamemodel.model.GameState;
import gamemodel.model.auxiliary.Direction;
import gamemodel.model.auxiliary.FieldSize;
import gametree.communication.GameMessage;
import gametree.communication.GameMessageDecoder;
import gametree.communication.handling.GameMessageHandler;
import gametree.communication.GameMessageEncoder;
import gametree.communication.GameMessageSender;
import gametree.communication.handling.NewGameStateHandler;
import gametree.communication.handling.NodeRoleManager;
import gametree.communication.messages.GameMessageFactory;
import network.announcement.AnnouncementSender;
import network.integrity.NodeActivityController;
import network.integrity.NodePinger;
import network.messages.auxiliary.Message;
import network.messages.auxiliary.PingMessage;
import network.messages.delivery.MessageDeliveryController;
import network.messages.MessageSender;
import network.messages.MessageReceiver;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameTreeNode {
    private static final long CONNECTION_TIMEOUT_MILLIS = 3000;
    private final Timer connectionTimer = new Timer("ConnectionTimer", true);
    private volatile TimerTask currentConnectionTimerTask;

    private static final int MULTICAST_PORT = 9192;
    private static final long ANNOUNCEMENT_MESSAGE_DELAY_MILLIS = 1000;
    private static final String MULTICAST_GROUP_ADDRESS = "239.192.0.4";
    private static final String CONFIG_FILEPATH = "config.properties";

    private Direction lastRequestedDirection;
    private GameTreeNodeData nodeData;
    private DatagramSocket socket;
    private MulticastSocket multicastSocket;

    private GameMessageSender gameMessageSender;
    private GameMessageHandler gameMessageHandler;

    private MessageDeliveryController messageDeliveryController;
    private NodeActivityController nodeActivityController;
    private NodePinger nodePinger;
    private NodeRoleManager nodeRoleManager;

    private GameStateController gameStateController;
    private NewGameStateHandler newGameStateHandler;

    private GameMessageEncoder gameMessageEncoder;
    private GameMessageDecoder gameMessageDecoder;

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    public GameTreeNode(GameMessageEncoder gameMessageEncoder,
                        GameMessageDecoder gameMessageDecoder) throws Exception {

        this.gameMessageEncoder = gameMessageEncoder;
        this.gameMessageDecoder = gameMessageDecoder;
        init();
    }

    public void setPlayerName(String playerName) {
        nodeData.setPlayerName(playerName);
    }

    public void startOwnGame() {
        lastRequestedDirection = null;
        gameMessageHandler.clearData();
        nodeData.startOwnGame(socket.getPort());
    }

    public void connect(ServerInfo serverInfo, boolean onlyView) throws IOException {
        lastRequestedDirection = null;
        nodeData.joinGame(serverInfo, onlyView);
        gameMessageHandler.addAllowedAddress(serverInfo.getInetSocketAddress());

        currentConnectionTimerTask = new TimerTask() {
            @Override
            public void run() {
                lastRequestedDirection = null;
                gameMessageHandler.clearData();

                nodeData.addInfoMessage(
                        String.format("Connection to %s:%d has failed",
                                serverInfo.getInetSocketAddress().getAddress().getHostAddress(),
                                serverInfo.getInetSocketAddress().getPort()
                        )
                );

                nodeData.leaveGame();
            }
        };

        // TODO: последующее удаление листенера
        nodeData.addPropertyChangeListener(GameTreeNodeData.Property.GAME_STARTED, evt -> {
            currentConnectionTimerTask.cancel();
        });
        connectionTimer.schedule(currentConnectionTimerTask, CONNECTION_TIMEOUT_MILLIS);

        GameMessage joinGameMessage = GameMessageFactory.createJoinMessage(
                serverInfo.getMasterID(), nodeData.getPlayerName(), onlyView
        );
        gameMessageSender.sendGameMessageWithoutAck(joinGameMessage, serverInfo.getInetSocketAddress());
    }

    public void leaveGame() throws IOException {
        lastRequestedDirection = null;
        gameMessageHandler.clearData();
        if (currentConnectionTimerTask != null) {
            currentConnectionTimerTask.cancel();
        }

        nodeRoleManager.handleGameExit();
    }

    public void setMovementDirection(Direction direction) throws IOException {
        synchronized (nodeData) {
            NodeRole nodeRole = nodeData.getPlayerNodeRole();
            if (nodeRole == NodeRole.VIEWER) {
                return;
            }

            if (direction == lastRequestedDirection) {
                return;
            }
            lastRequestedDirection = direction;

            int playerID = nodeData.getPlayerID();
            if (nodeRole == NodeRole.MASTER) {
                nodeData.setPlayerDirection(playerID, direction);
                return;
            }

            Integer masterID = nodeData.getMasterPlayerID();
            if (masterID == null) {
                System.err.println("Master id was not set yet");
                lastRequestedDirection = null;
                return;
            }

            GamePlayer masterPlayer;
            GameState gameState = nodeData.getGameState();
            if (gameState == null) {
                System.err.println("Can't set movement direction: game state was not set yet");
                lastRequestedDirection = null;
                return;
            }

            synchronized (gameState) {
                masterPlayer = gameState.getPlayers().get(masterID);
                if (masterPlayer == null) {
                    System.err.println("Master was not found");
                    lastRequestedDirection = null;
                    return;
                }

                if (masterPlayer.getNodeRole() != NodeRole.MASTER) {
                    System.err.println(
                            "Failed to set direction: master has died, " +
                                    "but the new one hasn't been chosen yet"
                    );
                    lastRequestedDirection = null;
                    return;
                }
            }

            GameMessage steerMessage = GameMessageFactory.createSteerMessage(playerID, masterID, direction);
            gameMessageSender.sendGameMessage(
                    steerMessage,
                    new InetSocketAddress(masterPlayer.getAddress(), masterPlayer.getPort())
            );
        }
    }

    public GameTreeNodeData getNodeData() {
        return nodeData;
    }

    public void shutdown() throws IOException {
        threadPool.shutdownNow();
        socket.close();
        multicastSocket.leaveGroup(InetAddress.getByName(MULTICAST_GROUP_ADDRESS));
        multicastSocket.close();
    }

    private void init() throws Exception {
        gameStateController = new GameStateController();
        threadPool.submit(gameStateController);

        GameConfig gameConfig = loadGameConfig();
        nodeData = new GameTreeNodeData(gameConfig, gameStateController);

        socket = new DatagramSocket();
        multicastSocket = new MulticastSocket(MULTICAST_PORT);
        multicastSocket.joinGroup(InetAddress.getByName(MULTICAST_GROUP_ADDRESS));

        MessageSender messageSender = new MessageSender(
                socket, new InetSocketAddress(MULTICAST_GROUP_ADDRESS, MULTICAST_PORT)
        );
        gameMessageSender = new GameMessageSender(messageSender, gameMessageEncoder);
        nodeRoleManager = new NodeRoleManager(nodeData, gameMessageSender);

        MessageReceiver messageReceiver = new MessageReceiver(socket, multicastSocket);
        threadPool.submit(messageReceiver);

        gameMessageHandler = new GameMessageHandler(
                messageReceiver, gameMessageDecoder, gameMessageSender, nodeRoleManager, nodeData
        );
        threadPool.submit(gameMessageHandler);

        AnnouncementSender announcementSender = new AnnouncementSender(
                ANNOUNCEMENT_MESSAGE_DELAY_MILLIS, messageSender,
                () -> {
                    GameState gameState = gameStateController.getGameState();
                    int playerID = nodeData.getPlayerID();
                    synchronized (gameState) {
                        return gameMessageEncoder.encodeGameMessage(
                                GameMessageFactory.createAnnouncementMessage(
                                        playerID, gameState
                                )
                        );
                    }
                }
        );
        threadPool.submit(announcementSender);

        newGameStateHandler = new NewGameStateHandler(nodeData, nodeRoleManager, gameMessageSender);
        gameStateController.setGameStateListener(newGameStateHandler);
        threadPool.submit(newGameStateHandler);

        messageDeliveryController = new MessageDeliveryController(
                gameConfig.getPingDelayMillis(), gameMessageHandler, messageSender
        );
        threadPool.submit(messageDeliveryController);

        nodeActivityController = new NodeActivityController(
                gameConfig.getNodeTimeoutMillis(), gameMessageHandler
        );
        threadPool.submit(nodeActivityController);

        nodePinger = new NodePinger(
                gameConfig.getPingDelayMillis(), messageSender, messageSender,
                receiverID -> {
                    var receiver = nodeData.getGameState().getPlayers().get(receiverID);
                    if (receiver == null) {
                        return null;
                    }

                    InetSocketAddress receiverSocketAddress = new InetSocketAddress(
                            receiver.getAddress(), receiver.getPort()
                    );

                    Message message = gameMessageEncoder.encodeGameMessage(
                            GameMessageFactory.createPingMessage(nodeData.getPlayerID(), receiverID)
                    );

                    PingMessage pingMessage = new PingMessage();
                    pingMessage.setMessage(message);
                    pingMessage.setReceiverSocketAddress(receiverSocketAddress);
                    return pingMessage;
                }
        );
        threadPool.submit(nodePinger);
    }

    private GameConfig loadGameConfig() throws GameLoadingException {
        Properties props = new Properties();
        try (InputStream configFile = getClass().getClassLoader().getResourceAsStream(CONFIG_FILEPATH)) {
            if (configFile == null) {
                throw new GameLoadingException(
                        String.format("Failed to open game config file \"%s\"", CONFIG_FILEPATH)
                );
            }
            props.load(configFile);
            return parseGameConfig(props);
        } catch (IOException | NumberFormatException e) {
            throw new GameLoadingException("Failed to load game config", e);
        }
    }

    private GameConfig parseGameConfig(Properties props) {
        GameConfig gameConfig = new GameConfig();

        gameConfig.setFieldSize(
                new FieldSize(
                        Integer.parseInt(props.getProperty("width")),
                        Integer.parseInt(props.getProperty("height"))
                )
        );

        gameConfig.setStaticFoodAmount(
                Integer.parseInt(props.getProperty("food_static"))
        );

        gameConfig.setFoodPerPlayer(
                Float.parseFloat(props.getProperty("food_per_player"))
        );

        gameConfig.setStateDelayMillis(
                Integer.parseInt(props.getProperty("state_delay_ms"))
        );

        gameConfig.setDeadFoodProbability(
                Float.parseFloat(props.getProperty("dead_food_prob"))
        );

        gameConfig.setPingDelayMillis(
                Integer.parseInt(props.getProperty("ping_delay_ms"))
        );

        gameConfig.setNodeTimeoutMillis(
                Integer.parseInt(props.getProperty("node_timeout_ms"))
        );

        return gameConfig;
    }
}
