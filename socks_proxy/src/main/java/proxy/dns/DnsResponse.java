package proxy.dns;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;

public class DnsResponse {
    private boolean isSuccess;
    private SelectionKey clientKey;
    private InetSocketAddress resolvedSocketAddress;

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public SelectionKey getClientKey() {
        return clientKey;
    }

    public void setClientKey(SelectionKey clientKey) {
        this.clientKey = clientKey;
    }

    public InetSocketAddress getResolvedSocketAddress() {
        return resolvedSocketAddress;
    }

    public void setResolvedSocketAddress(InetSocketAddress resolvedSocketAddress) {
        this.resolvedSocketAddress = resolvedSocketAddress;
    }
}
