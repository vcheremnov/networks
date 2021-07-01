package proxy.socks;

enum AddressType {
    IPV4,
    DOMAIN_NAME,
    IPV6;

    static AddressType getByValue(byte value) {
        switch (value) {
            case 0x01: return IPV4;
            case 0x03: return DOMAIN_NAME;
            case 0x04: return IPV6;
            default:   return null;
        }
    }

    byte getValue() {
        switch (this) {
            case IPV4: return 0x01;
            case DOMAIN_NAME: return 0x03;
            case IPV6: return 0x04;
            default: return 0x00;
        }
    }
}
