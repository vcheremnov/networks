package gametree.communication;

import network.messages.auxiliary.Message;

@FunctionalInterface
public interface GameMessageEncoder {
    Message encodeGameMessage(GameMessage gameMessage);
}
