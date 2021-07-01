package chatnode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HeartBeatChecker implements Runnable {
    private static final int HEART_BEAT_PERIOD_MILLIS = SendSession.TIMEOUT_MILLIS;
    private ChatNode chatNode;
    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    public HeartBeatChecker(ChatNode chatNode) {
        this.chatNode = chatNode;
        singleThreadExecutor.submit(this);
    }

    public void shutdown() {
        singleThreadExecutor.shutdownNow();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                SendSession sendSession = chatNode.getMessageSender().sendHeartBeatMessage();
                Thread.sleep(HEART_BEAT_PERIOD_MILLIS);
                if (sendSession != null) {
                    sendSession.awaitTermination();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.printf("Heart beat checker interruption error: %s\n", e.getMessage());
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }
}
