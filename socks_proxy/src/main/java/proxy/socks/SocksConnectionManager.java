package proxy.socks;

import proxy.KeyAttachment;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SocksConnectionManager {
    private static final int SOCKS_VERSION = 0x05;

    private static final int NO_AUTHENTICATION_CODE = 0x00;
    private static final int NO_SUPPORTED_AUTH_METHOD_CODE = 0xFF;

    private static final int IPV4_ADDRESS_LENGTH = 4;
    private static final int IPV6_ADDRESS_LENGTH = 16;

    private Map<SelectionKey, InetSocketAddress> requestedConnectionsMap = new HashMap<>();
    private Map<SelectionKey, ClientConnectionData> connectionDataMap = new HashMap<>();
    private Map<SelectionKey, AuthMethodsInfo> authMethodsInfoMap = new HashMap<>();
    private Map<SelectionKey, ConnectionStatus> connectionStatusMap = new HashMap<>();

    public void addClient(SelectionKey clientKey) {
        connectionDataMap.put(clientKey, new ClientConnectionData());
        authMethodsInfoMap.put(clientKey, new AuthMethodsInfo());
        connectionStatusMap.put(clientKey, ConnectionStatus.NOT_ESTABLISHED);
    }

    public void removeClient(SelectionKey clientKey) {
        connectionDataMap.remove(clientKey);
        connectionStatusMap.remove(clientKey);
        authMethodsInfoMap.remove(clientKey);
        requestedConnectionsMap.remove(clientKey);
    }

    public ConnectionStatus getConnectionStatus(SelectionKey clientKey) {
        return connectionStatusMap.get(clientKey);
    }

    public Map<SelectionKey, InetSocketAddress> getRequestedConnections() {
        return requestedConnectionsMap;
    }

    public void notifyClientOfRequestSuccess(SelectionKey clientKey) {
        connectionStatusMap.put(clientKey, ConnectionStatus.ESTABLISHED);
        writeResponse(clientKey, ResponseStatus.REQUEST_GRANTED);
    }

    public void notifyClientOfRequestFailure(SelectionKey clientKey) {
        connectionStatusMap.put(clientKey, ConnectionStatus.FAILED);
        writeResponse(clientKey, ResponseStatus.GENERAL_FAILURE);
    }

    public void fillHandShakeInfo(SelectionKey clientKey) {
        fillAuthMethodsInfo(clientKey);
        fillClientConnectionData(clientKey);
    }

    private void fillAuthMethodsInfo(SelectionKey clientKey) {
        KeyAttachment keyAttachment = (KeyAttachment) clientKey.attachment();
        ByteBuffer readBuffer = keyAttachment.getReadBuffer();

        AuthMethodsInfo authMethodsInfo = authMethodsInfoMap.get(clientKey);
        int currentFieldIndex = authMethodsInfo.getCurrentFieldIndex();
        switch (currentFieldIndex) {
            case AuthMethodsInfo.SOCKS_VERSION_FIELD_INDEX:
                if (readBuffer.remaining() >= Byte.BYTES) {
                    byte socksVersion = readBuffer.get();
                    authMethodsInfo.setSocksVersion(socksVersion);
                } else break;
            case AuthMethodsInfo.AUTH_METHODS_NUMBER_FIELD_INDEX:
                if (readBuffer.remaining() >= Byte.BYTES) {
                    byte authMethodsNumber = readBuffer.get();
                    authMethodsInfo.setAuthMethodsNumber(authMethodsNumber);
                } else break;
            case AuthMethodsInfo.AUTH_METHODS_FIELD_INDEX:
                authMethodsInfo.fillAuthMethods(readBuffer);
                if (authMethodsInfo.isFilled()) {
                    handleAuthenticationMessage(clientKey);
                }
            default: break;
        }
    }

    private void fillClientConnectionData(SelectionKey clientKey) {
        KeyAttachment keyAttachment = (KeyAttachment) clientKey.attachment();
        ByteBuffer readBuffer = keyAttachment.getReadBuffer();

        ClientConnectionData connectionData = connectionDataMap.get(clientKey);
        int currentFieldIndex = connectionData.getCurrentFieldIndex();
        switch (currentFieldIndex) {
            case ClientConnectionData.SOCKS_VERSION_FIELD_INDEX:
                if (readBuffer.remaining() >= Byte.BYTES) {
                    byte socksVersion = readBuffer.get();
                    connectionData.setSocksVersion(socksVersion);
                } else break;
            case ClientConnectionData.COMMAND_CODE_FIELD_INDEX:
                if (readBuffer.remaining() >= Byte.BYTES) {
                    byte commandCode = readBuffer.get();
                    connectionData.setCommandCode(commandCode);
                } else break;
            case ClientConnectionData.RESERVED_FIELD_INDEX:
                if (readBuffer.remaining() >= Byte.BYTES) {
                    readBuffer.get();
                    connectionData.skipReservedField();
                } else break;
            case ClientConnectionData.ADDRESS_TYPE_FIELD_INDEX:
                if (readBuffer.remaining() >= Byte.BYTES) {
                    byte addressTypeByte = readBuffer.get();
                    connectionData.setAddressType(addressTypeByte);
                    AddressType addressType = AddressType.getByValue(addressTypeByte);

                    if (addressType == null) {
                        handleConnectionMessage(clientKey);
                        break;
                    }

                    switch (addressType) {
                        case IPV4:
                            connectionData.setDestAddressLength(IPV4_ADDRESS_LENGTH);
                            break;
                        case IPV6:
                            connectionData.setDestAddressLength(IPV6_ADDRESS_LENGTH);
                            break;
                        case DOMAIN_NAME: default:
                            break;
                    }
                } else break;
            case ClientConnectionData.DEST_ADDRESS_FIELD_INDEX:
                if (readBuffer.remaining() >= Byte.BYTES) {
                    if (connectionData.getDestAddress() == null) {
                        // domain name length was not specified yet
                        byte addressLength = readBuffer.get();
                        connectionData.setDestAddressLength(addressLength);
                    }

                    connectionData.fillDestAddress(readBuffer);
                } else break;
            case ClientConnectionData.DEST_PORT_FIELD_INDEX:
                if (readBuffer.remaining() >= Short.BYTES) {
                    int port = readBuffer.getShort();
                    connectionData.setPort(port);

                    handleConnectionMessage(clientKey);
                }
            default: break;
        }
    }

    private void handleAuthenticationMessage(SelectionKey clientKey) {
        AuthMethodsInfo authMethodsInfo = authMethodsInfoMap.get(clientKey);
        byte[] authMethods = authMethodsInfo.getAuthMethods();

        int chosenAuthMethod = NO_SUPPORTED_AUTH_METHOD_CODE;
        for (int i = 0; i < authMethods.length; ++i) {
            if (authMethods[i] == (byte) NO_AUTHENTICATION_CODE) {
                chosenAuthMethod = NO_AUTHENTICATION_CODE;
                break;
            }
        }

        if (chosenAuthMethod == NO_SUPPORTED_AUTH_METHOD_CODE) {
            connectionStatusMap.put(clientKey, ConnectionStatus.FAILED);
        }

        writeAuthMethodsResponse(clientKey, chosenAuthMethod);
    }

    private void handleConnectionMessage(SelectionKey clientKey) {
        ClientConnectionData connectionData = connectionDataMap.get(clientKey);

        byte commandCodeByte = connectionData.getCommandCode();
        CommandType commandType = CommandType.getByValue(commandCodeByte);
        if (commandType != CommandType.ESTABLISH_CONNECTION) {
            connectionStatusMap.put(clientKey, ConnectionStatus.FAILED);
            writeResponse(clientKey, ResponseStatus.COMMAND_NOT_SUPPORTED);
            return;
        }

        byte addressTypeByte = connectionData.getAddressType();
        AddressType addressType = AddressType.getByValue(addressTypeByte);
        if (addressType == null || addressType == AddressType.IPV6) {
            connectionStatusMap.put(clientKey, ConnectionStatus.FAILED);
            writeResponse(clientKey, ResponseStatus.ADDRESS_TYPE_NOT_SUPPORTED);
            return;
        }

        int port = connectionData.getPort();
        InetSocketAddress serverSocketAddress = null;
        byte[] addressBytes = connectionData.getDestAddress();
        if (addressType == AddressType.IPV4) {
            try {
                InetAddress inetAddress = InetAddress.getByAddress(addressBytes);
                serverSocketAddress = new InetSocketAddress(inetAddress, port);
            } catch (UnknownHostException e) {
                throw new RuntimeException("IP address is of illegal length");
            }
        } else {
            char[] hostname = StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(addressBytes)).array();
            serverSocketAddress = InetSocketAddress.createUnresolved(String.copyValueOf(hostname), port);
        }

        requestedConnectionsMap.put(clientKey, serverSocketAddress);
    }

    private void writeAuthMethodsResponse(SelectionKey clientKey, int chosenAuthMethod) {
        clientKey.interestOpsOr(SelectionKey.OP_WRITE);

        KeyAttachment keyAttachment = (KeyAttachment) clientKey.attachment();
        ByteBuffer writeBuffer = keyAttachment.getWriteBuffer();

        writeBuffer.put((byte) SOCKS_VERSION);
        writeBuffer.put((byte) chosenAuthMethod);
    }

    private void writeResponse(SelectionKey clientKey, ResponseStatus status) {
        clientKey.interestOpsOr(SelectionKey.OP_WRITE);

        KeyAttachment keyAttachment = (KeyAttachment) clientKey.attachment();
        ByteBuffer writeBuffer = keyAttachment.getWriteBuffer();

        writeBuffer.put((byte) SOCKS_VERSION);
        writeBuffer.put(status.getValue());
        writeBuffer.put((byte) 0x00);
        writeBuffer.put(AddressType.IPV4.getValue());
        writeBuffer.put(new byte[]{0, 0, 0, 0});
        writeBuffer.putShort((short) 0);
    }
}
