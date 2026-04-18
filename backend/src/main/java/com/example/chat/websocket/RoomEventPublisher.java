package com.example.chat.websocket;

import com.example.chat.messages.dto.MessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class RoomEventPublisher {

    private final SimpMessagingTemplate template;

    public RoomEventPublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    private void publish(UUID roomId, Map<String, Object> event) {
        template.convertAndSend("/topic/rooms/" + roomId, event);
    }

    public void publishMessageNew(UUID roomId, MessageResponse msg) {
        publish(roomId, Map.of("type", "MESSAGE_NEW", "message", msg));
    }

    public void publishMessageEdited(UUID roomId, UUID messageId, String content, OffsetDateTime editedAt) {
        publish(roomId, Map.of("type", "MESSAGE_EDITED", "messageId", messageId, "content", content, "editedAt", editedAt));
    }

    public void publishMessageDeleted(UUID roomId, UUID messageId) {
        publish(roomId, Map.of("type", "MESSAGE_DELETED", "messageId", messageId));
    }

    public void publishMemberJoined(UUID roomId, UUID userId, String username) {
        publish(roomId, Map.of("type", "MEMBER_JOINED", "userId", userId, "username", username));
    }

    public void publishMemberLeft(UUID roomId, UUID userId, String username) {
        publish(roomId, Map.of("type", "MEMBER_LEFT", "userId", userId, "username", username));
    }

    public void publishMemberKicked(UUID roomId, UUID userId, String username) {
        publish(roomId, Map.of("type", "MEMBER_KICKED", "userId", userId, "username", username));
    }
}
