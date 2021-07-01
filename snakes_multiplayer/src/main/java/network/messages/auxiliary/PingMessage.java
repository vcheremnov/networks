package network.messages.auxiliary;

import java.net.InetSocketAddress;

public class PingMessage {
    private Message message;
    private InetSocketAddress receiverSocketAddress;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public InetSocketAddress getReceiverSocketAddress() {
        return receiverSocketAddress;
    }

    public void setReceiverSocketAddress(InetSocketAddress receiverSocketAddress) {
        this.receiverSocketAddress = receiverSocketAddress;
    }
}
