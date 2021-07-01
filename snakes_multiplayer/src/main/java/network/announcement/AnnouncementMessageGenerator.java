package network.announcement;

import network.messages.auxiliary.Message;

@FunctionalInterface
public interface AnnouncementMessageGenerator {
    Message getAnnouncementMessage() throws Exception;
}
