package network.integrity;

import network.messages.auxiliary.PingMessage;

@FunctionalInterface
public interface PingMessageEncoder {
    PingMessage getPingMessage(int receiverID);
}
