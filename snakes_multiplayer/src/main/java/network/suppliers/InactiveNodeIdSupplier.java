package network.suppliers;

@FunctionalInterface
public interface InactiveNodeIdSupplier {
    int getNextInactiveNodeID() throws InterruptedException;
}
