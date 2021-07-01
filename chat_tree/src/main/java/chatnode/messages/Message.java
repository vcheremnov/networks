package chatnode.messages;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

public class Message {
    private static final int MESSAGE_HEADER_SIZE = 20;

    public static final int ACK_TYPE = 0;
    public static final int CONNECT_TYPE = 1;
    public static final int RESERVE_NODE_TYPE = 2;
    public static final int TEXT_TYPE = 3;
    public static final int HEART_BEAT_TYPE = 4;

    private UUID messageUUID;
    private int messageType;

    private byte[] message;
    private byte[] messageBody;

    private InetSocketAddress remoteSocketAddress;

    public Message(byte[] message, InetSocketAddress remoteSocketAddress) {
        this.message = message;
        this.remoteSocketAddress = remoteSocketAddress;

        parseMessageHeader();
        retrieveMessageBody();
    }

    public Message(UUID messageUUID, int messageType, byte[] messageBody, InetSocketAddress remoteSocketAddress) {
        this.messageUUID = messageUUID;
        this.messageType = messageType;

        int messageLength = (messageBody == null) ? MESSAGE_HEADER_SIZE : MESSAGE_HEADER_SIZE + messageBody.length;
        message = new byte[messageLength];
        this.messageBody = messageBody;
        this.remoteSocketAddress = remoteSocketAddress;

        fillMessageHeader();
        fillMessageBody();
    }

    public UUID getUUID() {
        return messageUUID;
    }

    public int getType() {
        return messageType;
    }

    public byte[] getMessageBytes() {
        return message;
    }

    public byte[] getBodyBytes() {
        return messageBody;
    }

    public InetSocketAddress getRemoteSocketAddress() {
        return remoteSocketAddress;
    }

    private void fillMessageHeader() {
        ByteBuffer messageHeader = ByteBuffer.wrap(message);
        messageHeader.putLong(messageUUID.getMostSignificantBits());
        messageHeader.putLong(messageUUID.getLeastSignificantBits());
        messageHeader.putInt(messageType);
    }

    private void fillMessageBody() {
        if (messageBody != null) {
            System.arraycopy(messageBody, 0, message, MESSAGE_HEADER_SIZE, messageBody.length);
        }
    }

    private void parseMessageHeader() {
        ByteBuffer messageBuffer = ByteBuffer.wrap(message);
        long uuidHighBytes = messageBuffer.getLong();
        long uuidLowBytes = messageBuffer.getLong();
        messageUUID = new UUID(uuidHighBytes, uuidLowBytes);
        messageType = messageBuffer.getInt();
    }

    private void retrieveMessageBody() {
        if (message.length > MESSAGE_HEADER_SIZE) {
            messageBody = Arrays.copyOfRange(message, MESSAGE_HEADER_SIZE, message.length);
        }
    }
}
