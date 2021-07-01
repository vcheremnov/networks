package proxy.socks;

enum ResponseStatus {
    REQUEST_GRANTED(0x00),
    GENERAL_FAILURE(0x01),
    CONNECTION_NOT_ALLOWED(0x02),
    NETWORK_UNREACHABLE(0x03),
    HOST_UNREACHABLE(0x04),
    CONNECTION_REFUSED(0x05),
    TTL_EXPIRED(0x06),
    COMMAND_NOT_SUPPORTED(0x07),
    PROTOCOL_ERROR(0x07),
    ADDRESS_TYPE_NOT_SUPPORTED(0x08);

    private byte value;

    ResponseStatus(int value) {
        this.value = (byte) value;
    }

    byte getValue() {
        return value;
    }
}
