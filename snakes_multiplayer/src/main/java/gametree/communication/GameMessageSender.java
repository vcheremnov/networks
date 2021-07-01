package gametree.communication;

import network.messages.auxiliary.Message;
import network.messages.delivery.MessageDeliveryService;

import java.io.IOException;
import java.net.InetSocketAddress;

public class GameMessageSender {
    private GameMessageEncoder gameMessageEncoder;
    private MessageDeliveryService messageDeliveryService;

    public GameMessageSender(MessageDeliveryService messageDeliveryService,
                             GameMessageEncoder gameMessageEncoder) {
        this.messageDeliveryService = messageDeliveryService;
        this.gameMessageEncoder = gameMessageEncoder;
    }

    public void sendGameMessage(GameMessage gameMessage,
                                InetSocketAddress receiverAddress) throws IOException {
        Message message = gameMessageEncoder.encodeGameMessage(gameMessage);
        if (gameMessage.getMessageType().requiresAcknowledgement()) {
            messageDeliveryService.sendMessageWithAck(message, receiverAddress);
        } else {
            messageDeliveryService.sendMessage(message, receiverAddress);
        }
    }

    public void sendGameMessageWithoutAck(GameMessage gameMessage,
                                          InetSocketAddress receiverAddress) throws IOException {
        Message message = gameMessageEncoder.encodeGameMessage(gameMessage);
        messageDeliveryService.sendMessage(message, receiverAddress);
    }
}
