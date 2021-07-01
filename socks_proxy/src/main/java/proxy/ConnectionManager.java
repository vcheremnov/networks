package proxy;

import proxy.socks.ConnectionStatus;
import proxy.socks.SocksConnectionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;

public class ConnectionManager {
    private SocksConnectionManager socksConnectionManager = new SocksConnectionManager();
    private Map<Long, SelectionKey> selectionKeys = new HashMap<>();
    private Set<SelectionKey> closedConnectionsSet = new HashSet<>();

    public boolean isConnectionPresent(SelectionKey selectionKey) {
        KeyAttachment keyAttachment = (KeyAttachment) selectionKey.attachment();
        return keyAttachment != null && selectionKeys.containsKey(keyAttachment.getID());
    }

    public void addClientConnection(SelectionKey clientKey) {
        addConnection(clientKey, true);
        socksConnectionManager.addClient(clientKey);
    }

    public void addServerConnection(SelectionKey serverKey, SelectionKey clientKey) {
        addConnection(serverKey, false);
        KeyAttachment clientKeyAttachment = (KeyAttachment) clientKey.attachment();
        KeyAttachment serverKeyAttachment = (KeyAttachment) serverKey.attachment();

        clientKeyAttachment.setRemoteID(serverKeyAttachment.getID());
        serverKeyAttachment.setRemoteID(clientKeyAttachment.getID());
    }

    public Map<SelectionKey, InetSocketAddress> getRequestedConnections() {
        return socksConnectionManager.getRequestedConnections();
    }

    public void notifyClientOfRequestFailure(SelectionKey clientKey) {
        socksConnectionManager.notifyClientOfRequestFailure(clientKey);
    }

    public void notifyClientOfRequestSuccess(SelectionKey clientKey) {
        socksConnectionManager.notifyClientOfRequestSuccess(clientKey);
    }

    private void addConnection(SelectionKey selectionKey, boolean isClient) {
        KeyAttachment keyAttachment = new KeyAttachment(isClient);
        selectionKey.attach(keyAttachment);
        selectionKeys.put(keyAttachment.getID(), selectionKey);
    }

    public void closeConnection(SelectionKey selectionKey) {
        KeyAttachment keyAttachment = (KeyAttachment) selectionKey.attachment();
        Long id = keyAttachment.getID();

        selectionKeys.remove(id);
        closedConnectionsSet.add(selectionKey);
        selectionKey.cancel();

        Long remoteID = keyAttachment.getRemoteID();
        if (remoteID != null) {
            SelectionKey remoteKey = selectionKeys.remove(remoteID);
            closedConnectionsSet.add(remoteKey);
            remoteKey.cancel();
        }

        Long clientID = keyAttachment.isClient() ? id : remoteID;
        if (clientID != null) {
            socksConnectionManager.removeClient(selectionKey);
        }
    }

    public Set<SelectionKey> getClosedConnections() {
        return closedConnectionsSet;
    }

    public void readData(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        KeyAttachment keyAttachment = (KeyAttachment) selectionKey.attachment();

        ByteBuffer readBuffer = keyAttachment.getReadBuffer();
        int bytesRead = socketChannel.read(readBuffer);
        readBuffer.flip();

        if (keyAttachment.isClient()) {
            ConnectionStatus connectionStatus = socksConnectionManager.getConnectionStatus(selectionKey);
            if (connectionStatus == ConnectionStatus.NOT_ESTABLISHED) {
                socksConnectionManager.fillHandShakeInfo(selectionKey);
                readBuffer.compact();
                return;
            }
        }

        Long remoteID = keyAttachment.getRemoteID();
        SelectionKey remoteKey = selectionKeys.get(remoteID);
        remoteKey.interestOpsOr(SelectionKey.OP_WRITE);
        KeyAttachment remoteKeyAttachment = (KeyAttachment) remoteKey.attachment();
        ByteBuffer remoteWriteBuffer = remoteKeyAttachment.getWriteBuffer();

        int bytesToReadAvailable = readBuffer.remaining();
        int bytesToWriteAvailable = remoteWriteBuffer.remaining();
        int oldReadBufferLimit = readBuffer.limit();
        int newReadBufferLimit = (bytesToReadAvailable <= bytesToWriteAvailable) ?
                oldReadBufferLimit : (oldReadBufferLimit - (bytesToReadAvailable - bytesToWriteAvailable));

        readBuffer.limit(newReadBufferLimit);
        remoteWriteBuffer.put(readBuffer);
        readBuffer.limit(oldReadBufferLimit);

        readBuffer.compact();
        if (bytesRead == -1 && !readBuffer.hasRemaining()) {
            closeConnection(selectionKey);
        }
    }

    public void writeData(SelectionKey selectionKey) throws IOException {
        KeyAttachment keyAttachment = (KeyAttachment) selectionKey.attachment();
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        ByteBuffer writeBuffer = keyAttachment.getWriteBuffer();
        writeBuffer.flip();
        socketChannel.write(writeBuffer);
        boolean writeHasFinished = !writeBuffer.hasRemaining();
        writeBuffer.compact();

        if (writeHasFinished) {
            selectionKey.interestOpsAnd(~SelectionKey.OP_WRITE);
            if (keyAttachment.isClient()) {
                ConnectionStatus connectionStatus = socksConnectionManager.getConnectionStatus(selectionKey);
                if (connectionStatus == ConnectionStatus.FAILED) {
                    closeConnection(selectionKey);
                }
            }
        }
    }
}
