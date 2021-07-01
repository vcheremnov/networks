package network.announcement;

import network.messages.auxiliary.Message;
import network.messages.delivery.MessageDeliveryService;

public class AnnouncementSender implements Runnable {
    private long announcementDelayMillis;
    private MessageDeliveryService messageDeliveryService;
    private AnnouncementMessageGenerator announcementMessageGenerator;

    public AnnouncementSender(long announcementDelayMillis,
                              MessageDeliveryService messageDeliveryService,
                              AnnouncementMessageGenerator announcementMessageGenerator) {
        this.announcementDelayMillis = announcementDelayMillis;
        this.messageDeliveryService = messageDeliveryService;
        this.announcementMessageGenerator = announcementMessageGenerator;
    }

    @Override
    public void run() {
        try {
            int t = 0;
            while (!Thread.currentThread().isInterrupted()) {
                Message announcementMessage = announcementMessageGenerator.getAnnouncementMessage();
                messageDeliveryService.sendMulticastMessage(announcementMessage);
                Thread.sleep(announcementDelayMillis);
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
}
