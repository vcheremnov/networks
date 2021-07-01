package gametree.communication;

public enum GameMessageType {
    PING_MESSAGE,
    STEER_MESSAGE,
    ACK_MESSAGE,
    STATE_MESSAGE,
    ANNOUNCEMENT_MESSAGE,
    JOIN_MESSAGE,
    ERROR_MESSAGE,
    ROLE_CHANGE_MESSAGE;

    public boolean requiresAcknowledgement() {
        switch (this) {
            case ACK_MESSAGE: case ERROR_MESSAGE: case ANNOUNCEMENT_MESSAGE:
                return false;
            default:
                return true;
        }
    }
}
