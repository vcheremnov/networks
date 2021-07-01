package gametree.communication.handling;

import gamemodel.model.GamePlayer;
import gamemodel.model.GameState;
import gamemodel.logic.GameStateListener;
import gametree.GameTreeNodeData;
import gametree.communication.GameMessage;
import gametree.communication.GameMessageSender;
import gametree.communication.messages.GameMessageFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public class NewGameStateHandler implements Runnable, GameStateListener {
    private GameTreeNodeData nodeData;
    private NodeRoleManager nodeRoleManager;
    private GameMessageSender gameMessageSender;

    private GameState gameState;
    private Set<GamePlayer> killedPlayers = new HashSet<>();
    private boolean wasNotified = false;

    public NewGameStateHandler(GameTreeNodeData nodeData,
                               NodeRoleManager nodeRoleManager,
                               GameMessageSender gameMessageSender) {
        this.nodeData = nodeData;
        this.nodeRoleManager = nodeRoleManager;
        this.gameMessageSender = gameMessageSender;
    }

    @Override
    public void notifyOfNewGameState(GameState gameState, Collection<GamePlayer> killedPlayers) {
        synchronized (this) {
            this.gameState = gameState;
            this.killedPlayers.addAll(killedPlayers);
            wasNotified = true;
            notify();
        }
    }

    @Override
    public synchronized void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                while (!wasNotified) {
                    wait();
                }
                wasNotified = false;

                synchronized (nodeData) {
                    synchronized (gameState) {
                        generateGameStateMessages();
                        nodeRoleManager.handleKilledPlayers(killedPlayers);
                    }
                }

                killedPlayers.clear();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void generateGameStateMessages() throws IOException {
        int playerID = nodeData.getPlayerID();
        for (var player: gameState.getPlayers().values()) {
            if (player.getPlayerID() == playerID) {
                continue;
            }

            GameMessage stateMessage = GameMessageFactory.createStateMessage(
                    playerID, player.getPlayerID(), gameState
            );

            gameMessageSender.sendGameMessage(
                    stateMessage, new InetSocketAddress(player.getAddress(), player.getPort())
            );
        }
    }
}
