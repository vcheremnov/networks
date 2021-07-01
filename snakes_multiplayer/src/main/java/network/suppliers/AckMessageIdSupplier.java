package network.suppliers;

import network.messages.auxiliary.AckMessageID;

@FunctionalInterface
public interface AckMessageIdSupplier {
    AckMessageID getNextAckMessageID() throws InterruptedException;
}
