package gametree.communication.messages;

import gametree.communication.GameMessage;
import gametree.communication.GameMessageType;

public class ErrorGameMessage extends GameMessage {
    private String errorDescription;

    public ErrorGameMessage() {
        super(GameMessageType.ERROR_MESSAGE);
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
}
