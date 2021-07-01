package chatnode.messages.handlers;

import chatnode.ChatNode;
import chatnode.messages.Message;
import chatnode.messages.MessageHandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TextMessageHandler implements MessageHandler {
    private ChatNode chatNode;

    public TextMessageHandler(ChatNode chatNode) {
        this.chatNode = chatNode;
    }

    @Override
    public void handleMessage(Message message) {
        byte[] messageBody = message.getBodyBytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(messageBody);
        int senderNameLength = byteBuffer.getInt();
        int textMessageLength = byteBuffer.getInt();
        byte[] senderNameBytes = new byte[senderNameLength];
        byte[] textMessageBytes = new byte[textMessageLength];
        byteBuffer.get(senderNameBytes);
        byteBuffer.get(textMessageBytes);

        String senderName = new String(senderNameBytes, StandardCharsets.UTF_8);
        String textMessage = new String(textMessageBytes, StandardCharsets.UTF_8);
        System.out.println(String.format("%s: %s", senderName, textMessage));

        InetSocketAddress senderAddress = message.getRemoteSocketAddress();
        chatNode.getMessageSender().sendTextMessage(senderAddress, messageBody);
    }
}
