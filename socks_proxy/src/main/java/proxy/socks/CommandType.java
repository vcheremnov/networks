package proxy.socks;

enum CommandType {
    ESTABLISH_CONNECTION,
    ESTABLISH_TCP_PORT_BINDING,
    ASSOCIATE_UDP_PORT;

    static CommandType getByValue(byte value) {
        switch (value) {
            case 0x01: return ESTABLISH_CONNECTION;
            case 0x02: return ESTABLISH_TCP_PORT_BINDING;
            case 0x03: return ASSOCIATE_UDP_PORT;
            default:   return null;
        }
    }
}
