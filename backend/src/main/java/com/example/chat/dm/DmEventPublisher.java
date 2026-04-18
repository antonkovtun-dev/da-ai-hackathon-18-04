package com.example.chat.dm;

import com.example.chat.dm.dto.DmMessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class DmEventPublisher {

    private final SimpMessagingTemplate template;

    public DmEventPublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void publishMessageNew(UUID threadId, DmMessageResponse msg) {
        template.convertAndSend("/topic/dm/" + threadId, Map.of("type", "DM_MESSAGE_NEW", "message", msg));
    }

    public void publishMessageEdited(UUID threadId, UUID messageId, String content, OffsetDateTime editedAt) {
        template.convertAndSend("/topic/dm/" + threadId,
            Map.of("type", "DM_MESSAGE_EDITED", "messageId", messageId, "content", content, "editedAt", editedAt));
    }

    public void publishMessageDeleted(UUID threadId, UUID messageId) {
        template.convertAndSend("/topic/dm/" + threadId,
            Map.of("type", "DM_MESSAGE_DELETED", "messageId", messageId));
    }
}
