package proxy.socks;

import java.nio.ByteBuffer;

class ClientConnectionData {
    static final int SOCKS_VERSION_FIELD_INDEX = 0;
    static final int COMMAND_CODE_FIELD_INDEX = 1;
    static final int RESERVED_FIELD_INDEX = 2;
    static final int ADDRESS_TYPE_FIELD_INDEX = 3;
    static final int DEST_ADDRESS_FIELD_INDEX = 4;
    static final int DEST_PORT_FIELD_INDEX = 5;

    private int currentFieldIndex = 0;

    private byte socksVersion;
    private byte commandCode;

    private byte addressType;

    private byte[] destAddress;
    private int destAddressPos = 0;

    private int port;

    public byte getSocksVersion() {
        return socksVersion;
    }

    public byte getCommandCode() {
        return commandCode;
    }

    public byte getAddressType() {
        return addressType;
    }

    public byte[] getDestAddress() {
        return destAddress;
    }

    public int getPort() {
        return port;
    }

    public void setSocksVersion(byte socksVersion) {
        this.socksVersion = socksVersion;
        ++currentFieldIndex;
    }

    public void setCommandCode(byte commandCode) {
        this.commandCode = commandCode;
        ++currentFieldIndex;
    }

    public void skipReservedField() {
        ++currentFieldIndex;
    }

    public void setAddressType(byte addressType) {
        this.addressType = addressType;
        ++currentFieldIndex;
    }

    public void setDestAddressLength(int destAddressLength) {
        destAddress = new byte[destAddressLength];
    }

    public void fillDestAddress(ByteBuffer buffer) {
        int bytesAvailable = buffer.remaining();
        int bytesRemained = destAddress.length - destAddressPos;
        int bytesToFill = Math.min(bytesAvailable, bytesRemained);

        buffer.get(destAddress, destAddressPos, bytesToFill);
        destAddressPos += bytesToFill;

        if (destAddressPos == destAddress.length) {
            ++currentFieldIndex;
        }
    }

    public void setPort(int port) {
        this.port = port;
        ++currentFieldIndex;
    }

    public int getCurrentFieldIndex() {
        return currentFieldIndex;
    }
}
