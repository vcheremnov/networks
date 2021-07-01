package gametree.communication.handling;

import gamemodel.model.GamePlayer;
import gamemodel.model.GameState;
import gametree.GameTreeNodeData;
import gametree.NodeRole;
import gametree.communication.GameMessage;
import gametree.communication.GameMessageSender;
import gametree.communication.messages.GameMessageFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class NodeRoleManager {
    private final GameTreeNodeData nodeData;
    private GameMessageSender gameMessageSender;

    public NodeRoleManager(GameTreeNodeData nodeData,
                           GameMessageSender gameMessageSender) {
        this.nodeData = nodeData;
        this.gameMessageSender = gameMessageSender;
    }

    public void handleKilledPlayers(Collection<GamePlayer> killedPlayers) throws IOException {
        synchronized (nodeData) {
            int playerID = nodeData.getPlayerID();
            synchronized (nodeData.getGameState()) {
                for (var player: killedPlayers) {
                    if (playerID == player.getPlayerID()) {
                        continue;
                    }
                    sendRoleChangeMessageWithAck(player.getPlayerID(), null, NodeRole.VIEWER);
                }

                // TODO: раскомментить!
                handleRetiredPlayers(killedPlayers);
            }
        }
    }

    public void handleTimeoutPlayers(Collection<GamePlayer> timeoutPlayers) throws IOException {
        synchronized (nodeData) {
            synchronized (nodeData.getGameState()) {
                for (var player: timeoutPlayers) {
                    nodeData.removePlayer(player.getPlayerID());
                }

                handleRetiredPlayers(timeoutPlayers);
            }
        }
    }

    public void handleNodeRoleChange(Integer senderID, Integer receiverID,
                                     NodeRole senderRole, NodeRole receiverRole) throws IOException {
        synchronized (nodeData) {
            if (receiverRole != null) {
                if (receiverRole == NodeRole.VIEWER) {
                    // мастер сообщил о нашей смерти
                    System.err.println("YOU DIED");
                    nodeData.assignRoleToPlayer(receiverRole, receiverID);
                } else if (receiverRole == NodeRole.DEPUTY) {
                    // мастер назначил нас заместителем
                    nodeData.assignRoleToPlayer(receiverRole, receiverID);
                    if (senderRole == NodeRole.MASTER) {
                        System.err.println("YOU ARE NEW DEPUTY (GOT FROM NEW MASTER)");
                        // сообщение отправил вновь назначенный мастер
                        nodeData.assignRoleToPlayer(senderRole, senderID);
                    } else {
                        System.err.println("YOU ARE NEW DEPUTY (GOT FROM OLD MASTER)");
                    }
                } else if (receiverRole == NodeRole.MASTER && senderRole == NodeRole.VIEWER) {
                    // мастер сообщил о своем выходе/смерти и назначил мастером нас
                    nodeData.assignRoleToPlayer(senderRole, senderID);
                    nodeData.assignRoleToPlayer(receiverRole, receiverID);
                    System.err.println("MASTER DIED, YOU ARE NEW MASTER");

                    Integer deputyPlayerID = findNewDeputyPlayer();
                    for (var player: nodeData.getGameState().getPlayers().values()) {
                        if (player.getPlayerID() == nodeData.getPlayerID()) {
                            continue;
                        }

                        if (deputyPlayerID != null && deputyPlayerID.equals(player.getPlayerID())) {
                            sendRoleChangeMessageWithAck(player.getPlayerID(), NodeRole.MASTER, NodeRole.DEPUTY);
                        } else {
                            sendRoleChangeMessageWithAck(player.getPlayerID(), NodeRole.MASTER, null);
                        }
                    }
                }
            } else {
                if (senderRole == NodeRole.MASTER) {
                    // отправитель сообщает, что он теперь мастер
                    System.err.println(senderID + " IS NEW MASTER");
                    nodeData.assignRoleToPlayer(senderRole, senderID);
                    // TODO: каким-то образом переотправить все сообщения новому мастеру
                } else if (senderRole == NodeRole.VIEWER) {
                    // отправитель сообщает нам, как мастеру, о своем выходе
                    System.err.println(senderID + " IS VIEWER NOW");
                    nodeData.assignRoleToPlayer(senderRole, senderID);
                    Integer deputyPlayerID = nodeData.getDeputyPlayerID();
                    if (senderID.equals(deputyPlayerID)) {
                        deputyPlayerID = findNewDeputyPlayer();
                        if (deputyPlayerID != null) {
                            sendRoleChangeMessageWithAck(deputyPlayerID, null, NodeRole.DEPUTY);
                        }
                    }
                }
            }
        }
    }

    public void handleGameExit() throws IOException {
        synchronized (nodeData) {
            Integer playerID = nodeData.getPlayerID();
            NodeRole nodeRole = nodeData.getPlayerNodeRole();
            nodeData.assignRoleToPlayer(NodeRole.VIEWER, playerID);

            Integer receiverID;
            NodeRole receiverRole;
            if (nodeRole == NodeRole.MASTER) {
                receiverID = nodeData.getDeputyPlayerID();
                receiverRole = NodeRole.MASTER;
            } else {
                receiverID = nodeData.getMasterPlayerID();
                receiverRole = null;
            }

            GameState gameState = nodeData.getGameState();
            if (receiverID != null && gameState != null) {
                synchronized (gameState) {
                    var receiverPlayer = gameState.getPlayers().get(receiverID);
                    if (receiverPlayer == null) {
                        return;
                    }

                    if (receiverID.equals(receiverPlayer.getPlayerID()) &&
                        receiverPlayer.getNodeRole() != NodeRole.MASTER) {
                        System.err.println(
                                "Failed to notify master of game exit: " +
                                        "Master has died, but the new one hasn't been chosen yet"
                        );
                    } else {
                        sendRoleChangeMessageWithoutAck(receiverID, NodeRole.VIEWER, receiverRole);
                    }
                }
            }

            nodeData.leaveGame();
        }
    }

    public Integer findNewDeputyPlayer() {
        synchronized (nodeData) {
            synchronized (nodeData.getGameState()) {
                Integer deputyPlayerID = nodeData.getGameState().getPlayers().values().stream()
                        .filter(player -> player.getNodeRole() == NodeRole.NORMAL)
                        .findFirst()
                        .map(GamePlayer::getPlayerID)
                        .orElse(null);

                nodeData.assignRoleToPlayer(NodeRole.DEPUTY, deputyPlayerID);

                return deputyPlayerID;
            }
        }
    }

    public void sendRoleChangeMessageWithAck(Integer receiverID,
                                              NodeRole senderRole,
                                              NodeRole receiverRole) throws IOException {
        sendRoleChangeMessage(receiverID, senderRole, receiverRole, true);
    }

    public void sendRoleChangeMessageWithoutAck(Integer receiverID,
                                                 NodeRole senderRole,
                                                 NodeRole receiverRole) throws IOException {
        sendRoleChangeMessage(receiverID, senderRole, receiverRole, false);
    }

    private void sendRoleChangeMessage(Integer receiverID,
                                       NodeRole senderRole,
                                       NodeRole receiverRole,
                                       boolean isAckNeeded) throws IOException {
        Integer playerID = nodeData.getPlayerID();
        GameMessage roleChangeMessage = GameMessageFactory.createRoleChangeMessage(
                playerID, receiverID, senderRole, receiverRole
        );

        GamePlayer receiverPlayer = nodeData.getGameState().getPlayers().get(receiverID);
        if (receiverPlayer != null) {
            InetSocketAddress receiverInetSocketAddress = new InetSocketAddress(
                    receiverPlayer.getAddress(), receiverPlayer.getPort()
            );
            if (isAckNeeded) {
                gameMessageSender.sendGameMessage(roleChangeMessage, receiverInetSocketAddress);
            } else {
                gameMessageSender.sendGameMessageWithoutAck(roleChangeMessage, receiverInetSocketAddress);
            }
        }
    }

    private void handleRetiredPlayers(Collection<GamePlayer> killedPlayers) throws IOException {
        if (nodeData.getPlayerNodeRole() != NodeRole.MASTER) {
            return;
        }

        Integer playerID = nodeData.getPlayerID();
        Integer deputyPlayerID = nodeData.getDeputyPlayerID();

        Set<Integer> killedPlayerIdSet = killedPlayers.stream()
                .map(GamePlayer::getPlayerID)
                .collect(Collectors.toSet());

        if (killedPlayerIdSet.contains(playerID)) {
            nodeData.assignRoleToPlayer(NodeRole.VIEWER, playerID);
            nodeData.assignRoleToPlayer(NodeRole.MASTER, null);
            if (deputyPlayerID == null || killedPlayerIdSet.contains(deputyPlayerID)) {
                deputyPlayerID = findNewDeputyPlayer();
                if (deputyPlayerID == null) {
                    return;
                }
            }

            // TODO: каким-то образом остановить гейм стейт контроллер(мб пусть он сам это делает??)
            nodeData.assignRoleToPlayer(NodeRole.DEPUTY, null);
            nodeData.assignRoleToPlayer(NodeRole.MASTER, deputyPlayerID);
            sendRoleChangeMessageWithAck(deputyPlayerID, NodeRole.VIEWER, NodeRole.MASTER);
        } else if (deputyPlayerID != null && killedPlayerIdSet.contains(deputyPlayerID)) {
            deputyPlayerID = findNewDeputyPlayer();
            if (deputyPlayerID != null) {
                sendRoleChangeMessageWithAck(deputyPlayerID, null, NodeRole.DEPUTY);
            }
        }
    }
}
