package chatnode.messages.handlers;

import chatnode.ChatNode;
import chatnode.messages.Message;
import chatnode.messages.MessageHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class ReserveNodeMessageHandler implements MessageHandler {
    private static final int IPV4_ADDRESS_BYTES = 4;
    private ChatNode chatNode;

    public ReserveNodeMessageHandler(ChatNode chatNode) {
        this.chatNode = chatNode;
    }

    @Override
    public void handleMessage(Message message) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(message.getBodyBytes());
        byte[] addr = new byte[IPV4_ADDRESS_BYTES];
        byteBuffer.get(addr);
        int port = byteBuffer.getInt();

        InetSocketAddress ownAddress = chatNode.getMessageChannel().getSocketAddress();
        InetSocketAddress parentReserveNodeAddress = new InetSocketAddress(InetAddress.getByAddress(addr), port);

        chatNode.getTreeStructureInfo().setParentReserveNodeAddress(
            ownAddress.equals(parentReserveNodeAddress) ? null : parentReserveNodeAddress
        );
    }
}
