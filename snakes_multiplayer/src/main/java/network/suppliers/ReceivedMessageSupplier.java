package network.suppliers;

import network.messages.auxiliary.ReceivedMessage;

@FunctionalInterface
public interface ReceivedMessageSupplier {
    ReceivedMessage getNextReceivedMessage() throws InterruptedException;
}
