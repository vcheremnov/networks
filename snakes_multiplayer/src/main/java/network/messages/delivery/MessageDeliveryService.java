package network.messages.delivery;

import network.messages.auxiliary.Message;
import network.messages.auxiliary.SentMessage;
import network.suppliers.SentMessageSupplier;
import network.suppliers.SentMessageWithAckSupplier;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface MessageDeliveryService extends SentMessageWithAckSupplier, SentMessageSupplier {
    SentMessage sendMessage(Message message, InetSocketAddress socketAddress) throws IOException;

    SentMessage sendMessageWithAck(Message message, InetSocketAddress socketAddress) throws IOException;

    SentMessage sendMulticastMessage(Message message) throws IOException;
}
