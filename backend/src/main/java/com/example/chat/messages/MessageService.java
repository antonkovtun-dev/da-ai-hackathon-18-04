package com.example.chat.messages;

import com.example.chat.memberships.RoomMembership;
import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.memberships.RoomRole;
import com.example.chat.messages.dto.MessageResponse;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import com.example.chat.websocket.RoomEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoomEventPublisher eventPublisher;

    public MessageService(MessageRepository messageRepository, RoomMembershipRepository membershipRepository,
                          UserRepository userRepository, RoomEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public MessageResponse sendMessage(UUID roomId, UUID authorId, String content) {
        membershipRepository.findByRoomIdAndUserId(roomId, authorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));

        Message msg = new Message();
        msg.setRoomId(roomId);
        msg.setAuthorId(authorId);
        msg.setContent(content);
        msg = messageRepository.save(msg);

        User author = userRepository.findById(authorId).orElseThrow();
        MessageResponse resp = toResponse(msg, author.getUsername());
        eventPublisher.publishMessageNew(roomId, resp);
        return resp;
    }

    @Transactional
    public MessageResponse editMessage(UUID messageId, UUID requesterId, String content) {
        Message msg = findMessageOrThrow(messageId);
        if (!msg.getAuthorId().equals(requesterId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot edit another user's message");
        msg.setContent(content);
        msg.setEditedAt(OffsetDateTime.now());
        msg = messageRepository.save(msg);
        eventPublisher.publishMessageEdited(msg.getRoomId(), messageId, content, msg.getEditedAt());
        User author = userRepository.findById(requesterId).orElseThrow();
        return toResponse(msg, author.getUsername());
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID requesterId) {
        Message msg = findMessageOrThrow(messageId);
        boolean isAuthor = msg.getAuthorId().equals(requesterId);
        if (!isAuthor) {
            RoomMembership m = membershipRepository.findByRoomIdAndUserId(msg.getRoomId(), requesterId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
            if (m.getRole() == RoomRole.MEMBER)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete another user's message");
        }
        msg.setContent(null);
        msg.setDeletedAt(OffsetDateTime.now());
        messageRepository.save(msg);
        eventPublisher.publishMessageDeleted(msg.getRoomId(), messageId);
    }

    public List<MessageResponse> getMessages(UUID roomId, UUID requesterId, UUID before, int limit) {
        membershipRepository.findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));

        List<Message> messages;
        if (before == null) {
            messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(
                    roomId, PageRequest.of(0, limit));
        } else {
            Message cursor = messageRepository.findById(before)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor"));
            messages = messageRepository.findByRoomIdBefore(
                    roomId, cursor.getCreatedAt(), PageRequest.of(0, limit));
        }

        Set<UUID> authorIds = messages.stream().map(Message::getAuthorId).collect(Collectors.toSet());
        Map<UUID, String> usernames = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return messages.stream()
                .map(m -> toResponse(m, usernames.getOrDefault(m.getAuthorId(), "unknown")))
                .toList();
    }

    private Message findMessageOrThrow(UUID id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
    }

    private MessageResponse toResponse(Message m, String authorUsername) {
        return new MessageResponse(m.getId(), m.getRoomId(), m.getAuthorId(), authorUsername,
                m.getContent(), m.getCreatedAt(), m.getEditedAt(), m.getDeletedAt() != null);
    }
}
