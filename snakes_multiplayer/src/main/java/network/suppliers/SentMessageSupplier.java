package network.suppliers;

import network.messages.auxiliary.SentMessage;

@FunctionalInterface
public interface SentMessageSupplier {
    SentMessage getNextSentMessage() throws InterruptedException;
}
