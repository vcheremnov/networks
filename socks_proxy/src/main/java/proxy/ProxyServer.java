package proxy;

import proxy.dns.DnsResponse;
import proxy.dns.DomainNameResolver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.*;

public class ProxyServer implements Runnable, AutoCloseable {
    private ServerSocketChannel serverSocketChannel;
    private DatagramChannel datagramChannel;
    private Selector selector;

    private DomainNameResolver domainNameResolver;
    private SelectionKey dnsSelectionKey;

    private ConnectionManager connectionManager;
    private Map<SelectionKey, SelectionKey> requestedServerKeyToClientKeyMap = new HashMap<>();

    public ProxyServer(int port) throws IOException {
        selector = Selector.open();

        connectionManager = new ConnectionManager();

        datagramChannel = DatagramChannel.open();
        datagramChannel.bind(null);
        datagramChannel.configureBlocking(false);
        dnsSelectionKey = datagramChannel.register(
                selector, SelectionKey.OP_READ
        );
        domainNameResolver = new DomainNameResolver(dnsSelectionKey);

        SocketAddress serverSocketAddress = new InetSocketAddress(port);
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(serverSocketAddress);
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.equals(dnsSelectionKey)) {
                        handleDnsEvents();
                        continue;
                    }

                    try {
                        if (key.isAcceptable()) {
                            createClientConnection();
                        }

                        if (key.isConnectable()) {
                            finishConnectionProcess(key);
                        }

                        if (key.isReadable()) {
                            connectionManager.readData(key);
                        }

                        if (key.isWritable()) {
                            connectionManager.writeData(key);
                        }
                    } catch (Exception e) {
                        closeConnection(key);

                        if (connectionManager.isConnectionPresent(key)) {
                            connectionManager.closeConnection(key);
                        }
                    }
                }

                handleRequestedConnections();
                handleClosedConnections();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void handleRequestedConnections() {
        var requestedConnectionsMap = connectionManager.getRequestedConnections();
        for (var clientKey: requestedConnectionsMap.keySet()) {
            InetSocketAddress serverSocketAddress = requestedConnectionsMap.get(clientKey);
            if (serverSocketAddress.isUnresolved()) {
                boolean requestWasMade = domainNameResolver.addRequestToQueue(clientKey, serverSocketAddress);
                if (!requestWasMade) {
                    connectionManager.notifyClientOfRequestFailure(clientKey);
                }
            } else {
                createServerConnection(clientKey, serverSocketAddress);
            }
        }

        requestedConnectionsMap.clear();
    }

    private void handleDnsEvents() throws IOException {
        if (dnsSelectionKey.isReadable()) {
            domainNameResolver.readResponses();
        }

        if (dnsSelectionKey.isWritable()) {
            domainNameResolver.writeRequests();
        }

        var dnsResponseSet = domainNameResolver.getDnsResponseSet();
        for (DnsResponse response: dnsResponseSet) {
            SelectionKey clientKey = response.getClientKey();
            if (response.isSuccess()) {
                InetSocketAddress serverSocketAddress = response.getResolvedSocketAddress();
                createServerConnection(clientKey, serverSocketAddress);
            } else {
                connectionManager.notifyClientOfRequestFailure(clientKey);
            }
        }

        dnsResponseSet.clear();
    }

    private void handleClosedConnections() {
        var closedConnectionsSet = connectionManager.getClosedConnections();
        closedConnectionsSet.forEach(this::closeConnection);
        closedConnectionsSet.clear();
    }

    private void finishConnectionProcess(SelectionKey serverKey) throws IOException {
        SelectionKey clientKey = requestedServerKeyToClientKeyMap.remove(serverKey);
        SocketChannel socketChannel = (SocketChannel) serverKey.channel();
        try {
            socketChannel.finishConnect();
            serverKey.interestOpsAnd(~SelectionKey.OP_CONNECT);
            connectionManager.addServerConnection(serverKey, clientKey);
            connectionManager.notifyClientOfRequestSuccess(clientKey);
        } catch (IOException e) {
            connectionManager.notifyClientOfRequestFailure(clientKey);
            throw new IOException(e);
        }
    }

    private void createClientConnection() {
        SocketChannel socketChannel = null;
        try {
            socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            SelectionKey clientKey = socketChannel.register(selector, SelectionKey.OP_READ);
            connectionManager.addClientConnection(clientKey);
        } catch (IOException e) {
            if (socketChannel != null) {
                closeChannel(socketChannel);
            }
        }
    }

    private void createServerConnection(SelectionKey clientKey, InetSocketAddress serverSocketAddress) {
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(serverSocketAddress);
            SelectionKey serverKey = socketChannel.register(
                    selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT
            );
            requestedServerKeyToClientKeyMap.put(serverKey, clientKey);
        } catch (IOException e) {
            if (socketChannel != null) {
                closeChannel(socketChannel);
            }
            connectionManager.notifyClientOfRequestFailure(clientKey);
        }
    }

    private void closeConnection(SelectionKey key) {
        key.cancel();
        closeChannel(key.channel());
    }

    private void closeChannel(Channel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void close() {
        if (selector != null) {
            selector.keys().forEach(this::closeConnection);
            try {
                selector.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        if (datagramChannel != null) {
            closeChannel(datagramChannel);
        }

        if (serverSocketChannel != null) {
            closeChannel(serverSocketChannel);
        }
    }
}
