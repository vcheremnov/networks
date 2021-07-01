package network.integrity;

import network.messages.auxiliary.HandledMessage;
import network.suppliers.HandledMessageSupplier;
import network.suppliers.InactiveNodeIdSupplier;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class NodeActivityController implements Runnable, InactiveNodeIdSupplier {
    private final int timeoutMillis;
    private Map<Integer, NodeActivity> nodeActivitiesMap = new HashMap<>();
    private SortedSet<NodeActivity> nodeActivitiesSet = new TreeSet<>();
    private BlockingQueue<Integer> inactiveNodes = new LinkedBlockingQueue<>();

    private ExecutorService handledMessageInfoListener = Executors.newSingleThreadExecutor();
    private HandledMessageSupplier handledMessageSupplier;

    private static class NodeActivity implements Comparable<NodeActivity> {
        private int nodeID;
        private long lastActivityTime;

        NodeActivity(int nodeID, long lastActivityTime) {
            this.nodeID = nodeID;
            this.lastActivityTime = lastActivityTime;
        }

        @Override
        public int compareTo(NodeActivity o) {
            return Long.compare(lastActivityTime, o.lastActivityTime);
        }

        int getNodeID() {
            return nodeID;
        }

        long getInactivityTime() {
            return System.currentTimeMillis() - lastActivityTime;
        }

        long getLastActivityTime() {
            return lastActivityTime;
        }

        void setLastActivityTime(long lastActivityTime) {
            this.lastActivityTime = lastActivityTime;
        }
    }

    public NodeActivityController(int timeoutMillis,
                                  HandledMessageSupplier handledMessageSupplier) {
        this.timeoutMillis = timeoutMillis;
        this.handledMessageSupplier = handledMessageSupplier;
    }

    @Override
    public int getNextInactiveNodeID() throws InterruptedException {
        return inactiveNodes.take();
    }

    public synchronized void addNodeID(int nodeID, long lastActivityTime) {
        if (nodeActivitiesMap.containsKey(nodeID)) {
            NodeActivity nodeActivity = nodeActivitiesMap.get(nodeID);
            if (nodeActivity.getLastActivityTime() < lastActivityTime) {
                nodeActivitiesSet.remove(nodeActivity);
                nodeActivity.setLastActivityTime(lastActivityTime);
                nodeActivitiesSet.add(nodeActivity);
            }
            return;
        }

        NodeActivity nodeActivity = new NodeActivity(nodeID, lastActivityTime);
        nodeActivitiesMap.put(nodeID, nodeActivity);
        nodeActivitiesSet.add(nodeActivity);

        notify();
    }

    public synchronized void removeNodeID(int nodeID) {
        NodeActivity nodeActivity = nodeActivitiesMap.remove(nodeID);
        if (nodeActivity != null) {
            nodeActivitiesSet.remove(nodeActivity);
        }
    }

    @Override
    public synchronized void run() {
        try {
            startAuxiliaryThreads();
            while (!Thread.currentThread().isInterrupted()) {
                long timeToSleep = calculateTimeToSleep();
                while (timeToSleep >= 0) {
                    wait(timeToSleep);
                    timeToSleep = calculateTimeToSleep();
                }

                handleInactiveNodes();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            stopAuxiliaryThreads();
        }
    }

    private long calculateTimeToSleep() {
        if (nodeActivitiesSet.isEmpty()) {
            return 0;
        }

        NodeActivity nodeActivity = nodeActivitiesSet.first();
        long inactivityTime = nodeActivity.getInactivityTime();
        long timeRemained = timeoutMillis - inactivityTime;

        return timeRemained == 0 ? -1 : timeRemained;
    }

    private void handleInactiveNodes() {
        Iterator<NodeActivity> setIterator = nodeActivitiesSet.iterator();
        while (setIterator.hasNext()) {
            NodeActivity nodeActivity = setIterator.next();
            long inactivityTime = nodeActivity.getInactivityTime();
            if (inactivityTime < timeoutMillis) {
                break;
            }

            int nodeID = nodeActivity.getNodeID();
            nodeActivitiesMap.remove(nodeID);
            setIterator.remove();

            inactiveNodes.add(nodeID);
        }
    }

    private void startAuxiliaryThreads() {
        handledMessageInfoListener.submit(this::listenNodesActivity);
    }

    private void listenNodesActivity() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                HandledMessage handledMessage = handledMessageSupplier.getNextHandledMessage();
                Integer nodeID = handledMessage.getMessage().getSenderID();
                if (nodeID == null) {
                    continue;
                }

                synchronized (this) {
                    NodeActivity nodeActivity;
                    Long lastActivityTime = handledMessage.getReceiveTimestamp();
                    if (nodeActivitiesMap.containsKey(nodeID)) {
                        nodeActivity = nodeActivitiesMap.get(nodeID);
                        nodeActivitiesSet.remove(nodeActivity);
                        nodeActivity.setLastActivityTime(lastActivityTime);
                    } else {
                        nodeActivity = new NodeActivity(nodeID, lastActivityTime);
                        nodeActivitiesMap.put(nodeID, nodeActivity);
                    }
                    nodeActivitiesSet.add(nodeActivity);

                    notify();
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }

    private void stopAuxiliaryThreads() {
        handledMessageInfoListener.shutdownNow();
    }
}
