package chatnode;

import chatnode.messages.MessageChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

/*
    Communication protocol:
    0) Big-endian byte order is used
    1) Message structure:
        * 8 bytes - most-significant bits of GUID
        * 8 bytes - least-significant bits of GUID
        * 4 bytes - message type identifier
        * message body (structure depends on a message type)
    1) Message types description:
        * 0 - message reception acknowledgement
            Sent as a response to every message in the system from a receiver to a sender.

            The message body is empty.

        * 1 - connection request
            Sent from a newly created node to a parent node.
            Reserve node info is sent as a response from the parent node (in an ack message).

            The message body is empty.

        * 2 - reserve node info message
            Sent from a parent to all its descendants when its reserve node has changed.
            If there is no reserve node, zero ip/port are sent.

            Message body structure:
            # 4 bytes - IPv4 address of a reserve node
            # 4 bytes - port of the reserve node

        * 3 - text message
            # 4 bytes - UTF-8 sender name length (N bytes)
            # 4 bytes - UTF-8 chat message string length (M bytes)
            # N bytes - sender name
            # M bytes - chat message

        * 4 - heart-beat check message
            The message body is empty.
 */

public class ChatNode {
    private final TreeStructureInfo treeStructureInfo = new TreeStructureInfo();

    private HeartBeatChecker heartBeatChecker;
    private MessageReceiver messageReceiver;
    private MessageSender messageSender;

    private MessageChannel messageChannel;
    private String nodeName;

    public ChatNode(String nodeName) {
        this.nodeName = nodeName;
    }

    public void start(String hostname, int nodePort, int lossPercentage) {
        try {
            messageChannel = new MessageChannel(hostname, nodePort, lossPercentage);
            messageSender = new MessageSender(this);
            messageReceiver = new MessageReceiver(this);
            heartBeatChecker = new HeartBeatChecker(this);

            if (isParentSet()) {
                SendSession sendSession = messageSender.sendConnectMessage();
                sendSession.awaitTermination();
            }

            try (BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {
                String textMessage = null;
                while ((textMessage = stdin.readLine()) != null) {
                    System.out.println(String.format("%s: %s", nodeName, textMessage));
                    messageSender.sendTextMessage(nodeName, textMessage);
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.printf("Chat node error: %s\n", e.getMessage());
        } finally {
            shutdown();
        }
    }

    public boolean isParentSet() {
        return treeStructureInfo.getParentAddress() != null;
    }

    public void setParent(InetSocketAddress parentAddress) {
        synchronized (treeStructureInfo) {
            treeStructureInfo.setParentAddress(parentAddress);
            treeStructureInfo.setReserveNodeAddress(parentAddress);
            if (parentAddress != null) {
                treeStructureInfo.getNeighbours().add(parentAddress);
            }
        }
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    public TreeStructureInfo getTreeStructureInfo() {
        return treeStructureInfo;
    }

    public MessageChannel getMessageChannel() {
        return messageChannel;
    }

    private void shutdown() {
        if (heartBeatChecker != null) {
            heartBeatChecker.shutdown();
        }

        if (messageReceiver != null) {
            messageReceiver.shutdown();
        }

        if (messageSender != null) {
            messageSender.shutdown();
        }

        if (messageChannel != null) {
            messageChannel.close();
        }
    }
}
