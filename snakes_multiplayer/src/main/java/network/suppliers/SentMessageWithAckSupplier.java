package network.suppliers;

import network.messages.auxiliary.SentMessage;

@FunctionalInterface
public interface SentMessageWithAckSupplier {
    SentMessage getNextSentMessageWithAck() throws InterruptedException;
}
