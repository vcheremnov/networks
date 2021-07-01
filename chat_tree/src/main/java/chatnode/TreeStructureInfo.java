package chatnode;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class TreeStructureInfo {
    private volatile InetSocketAddress parentAddress;
    private volatile InetSocketAddress parentReserveNodeAddress;
    private volatile InetSocketAddress reserveNodeAddress;
    private volatile Set<InetSocketAddress> neighbours = new HashSet<>();

    public synchronized InetSocketAddress getParentAddress() {
        return parentAddress;
    }

    public synchronized void setParentAddress(InetSocketAddress parentAddress) {
        this.parentAddress = parentAddress;
    }

    public synchronized InetSocketAddress getParentReserveNodeAddress() {
        return parentReserveNodeAddress;
    }

    public synchronized void setParentReserveNodeAddress(InetSocketAddress parentReserveNodeAddress) {
        this.parentReserveNodeAddress = parentReserveNodeAddress;
    }

    public synchronized InetSocketAddress getReserveNodeAddress() {
        return reserveNodeAddress;
    }

    public synchronized void setReserveNodeAddress(InetSocketAddress reserveNodeAddress) {
        this.reserveNodeAddress = reserveNodeAddress;
    }

    public synchronized Set<InetSocketAddress> getNeighbours() {
        return neighbours;
    }
}
