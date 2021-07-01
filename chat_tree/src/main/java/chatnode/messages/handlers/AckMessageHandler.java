package chatnode.messages.handlers;

import chatnode.ChatNode;
import chatnode.messages.Message;
import chatnode.messages.MessageHandler;

public class AckMessageHandler implements MessageHandler {
    private ChatNode chatNode;

    public AckMessageHandler(ChatNode chatNode) {
        this.chatNode = chatNode;
    }

    @Override
    public void handleMessage(Message message) {
        chatNode.getMessageSender().addDeliveredMessage(message.getUUID());
    }
}
