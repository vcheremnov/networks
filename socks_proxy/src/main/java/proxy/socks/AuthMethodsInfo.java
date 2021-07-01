package proxy.socks;

import java.nio.ByteBuffer;

public class AuthMethodsInfo {
    private static final int FIELDS_NUMBER = 3;
    static final int SOCKS_VERSION_FIELD_INDEX = 0;
    static final int AUTH_METHODS_NUMBER_FIELD_INDEX = 1;
    static final int AUTH_METHODS_FIELD_INDEX = 2;
    private int currentFieldIndex = 0;

    private byte socksVersion;
    private byte[] authMethods;
    private int authMethodsPos = 0;

    public byte getSocksVersion() {
        return socksVersion;
    }

    public int getAuthMethodsNumber() {
        return authMethods.length;
    }

    public byte[] getAuthMethods() {
        return authMethods;
    }

    public void setSocksVersion(Byte socksVersion) {
        this.socksVersion = socksVersion;
        ++currentFieldIndex;
    }

    public void setAuthMethodsNumber(Byte authMethodsNumber) {
        authMethods = new byte[authMethodsNumber];
        ++currentFieldIndex;
    }

    public void fillAuthMethods(ByteBuffer buffer) {
        int bytesAvailable = buffer.remaining();
        int authMethodsRemained = authMethods.length - authMethodsPos;
        int authMethodsToFill = Math.min(bytesAvailable, authMethodsRemained);

        buffer.get(authMethods, authMethodsPos, authMethodsToFill);
        authMethodsPos += authMethodsToFill;

        if (authMethodsPos == authMethods.length) {
            ++currentFieldIndex;
        }
    }

    public int getCurrentFieldIndex() {
        return currentFieldIndex;
    }

    public boolean isFilled() {
        return currentFieldIndex == FIELDS_NUMBER;
    }
}
