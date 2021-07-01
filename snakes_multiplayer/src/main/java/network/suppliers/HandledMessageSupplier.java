package network.suppliers;

import network.messages.auxiliary.HandledMessage;

@FunctionalInterface
public interface HandledMessageSupplier {
    HandledMessage getNextHandledMessage() throws InterruptedException;
}
