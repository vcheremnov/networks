package chatnode.messages.handlers;

import chatnode.ChatNode;
import chatnode.TreeStructureInfo;
import chatnode.messages.Message;
import chatnode.messages.MessageHandler;

import java.net.InetSocketAddress;
import java.util.Set;

public class ConnectMessageHandler implements MessageHandler {
    private ChatNode chatNode;

    public ConnectMessageHandler(ChatNode chatNode) {
        this.chatNode = chatNode;
    }

    @Override
    public void handleMessage(Message message) {
        InetSocketAddress remoteSocketAddress = message.getRemoteSocketAddress();
        TreeStructureInfo treeStructureInfo = chatNode.getTreeStructureInfo();
        synchronized (treeStructureInfo) {
            Set<InetSocketAddress> neighbours = treeStructureInfo.getNeighbours();
            if (neighbours.isEmpty()) {
                treeStructureInfo.setReserveNodeAddress(remoteSocketAddress);
            }
            neighbours.add(remoteSocketAddress);
        }
        chatNode.getMessageSender().sendReserveNodeMessage(remoteSocketAddress);
    }
}
