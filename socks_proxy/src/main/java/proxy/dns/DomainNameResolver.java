package proxy.dns;

import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.*;

public class DomainNameResolver {
    private static int nextID = 0;

    private SelectionKey selectionKey;
    private DatagramChannel datagramChannel;
    private InetSocketAddress resolverSocketAddress;

    private HashMap<Integer, Integer> idToServerPortMap = new HashMap<>();
    private HashMap<Integer, SelectionKey> idToClientKeyMap = new HashMap<>();

    private static final int BUFFER_LENGTH = 65536;
    private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_LENGTH);
    private Queue<byte[]> requestMessageQueue = new ArrayDeque<>();
    private Set<DnsResponse> dnsResponseSet = new HashSet<>();

    public DomainNameResolver(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
        datagramChannel = (DatagramChannel) selectionKey.channel();
        resolverSocketAddress = new InetSocketAddress(
                ResolverConfig.getCurrentConfig().server(), 53
        );
    }

    public boolean addRequestToQueue(SelectionKey clientKey, InetSocketAddress unresolvedSocketAddress) {
        int requestID = getNextID();
        byte[] requestMessage = createDnsRequestMessage(requestID, unresolvedSocketAddress.getHostName());
        if (requestMessage == null) {
            return false;
        }

        idToClientKeyMap.put(requestID, clientKey);
        idToServerPortMap.put(requestID, unresolvedSocketAddress.getPort());

        selectionKey.interestOpsOr(SelectionKey.OP_WRITE);
        requestMessageQueue.add(requestMessage);

        return true;
    }

    public Set<DnsResponse> getDnsResponseSet() {
        return dnsResponseSet;
    }

    public void readResponses() throws IOException {
        while (datagramChannel.receive(buffer) != null) {
            buffer.flip();
            int responseLength = buffer.remaining();
            byte[] responseMessage = new byte[responseLength];
            buffer.get(responseMessage);
            buffer.clear();

            DnsResponse response = getResponseInfo(responseMessage);
            dnsResponseSet.add(response);
        };
    }

    public void writeRequests() throws IOException {
        var queueIterator = requestMessageQueue.iterator();
        while (queueIterator.hasNext()) {
            byte[] requestMessage = queueIterator.next();
            buffer.put(requestMessage);
            buffer.flip();
            int bytesSent = datagramChannel.send(buffer, resolverSocketAddress);
            buffer.clear();

            if (bytesSent == 0) {
                return;
            }

            queueIterator.remove();
        }

        selectionKey.interestOpsAnd(~SelectionKey.OP_WRITE);
    }

    private int getNextID() {
        return nextID++;
    }

    private byte[] createDnsRequestMessage(int requestID, String domainName) {
        try {
            String absoluteName = domainName.charAt(domainName.length() - 1) == '.' ?
                    domainName : domainName + ".";
            Record aRecord = ARecord.newRecord(Name.fromString(absoluteName), Type.A, DClass.IN);
            Message requestMessage = Message.newQuery(aRecord);
            requestMessage.getHeader().setID(requestID);
            return requestMessage.toWire();
        } catch (Exception e) {
            return null;
        }
    }

    private DnsResponse getResponseInfo(byte[] responseMessage) throws IOException {
        Message response = new Message(responseMessage);
        Record[] answerRecords = response.getSectionArray(Section.ANSWER);
        InetAddress resolvedAddress = Arrays.stream(answerRecords)
                .filter(record -> record.getType() == Type.A)
                .findFirst()
                .map(ARecord.class::cast)
                .map(ARecord::getAddress)
                .orElse(null);

        int id = response.getHeader().getID();
        SelectionKey clientKey = idToClientKeyMap.remove(id);
        int serverPort = idToServerPortMap.remove(id);
        DnsResponse dnsResponse = new DnsResponse();
        dnsResponse.setClientKey(clientKey);

        if (resolvedAddress != null) {
            InetSocketAddress resolvedSocketAddress = new InetSocketAddress(resolvedAddress, serverPort);
            dnsResponse.setResolvedSocketAddress(resolvedSocketAddress);
            dnsResponse.setSuccess(true);
        } else {
            dnsResponse.setSuccess(false);
        }

        return dnsResponse;
    }
}
