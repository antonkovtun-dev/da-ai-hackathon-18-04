package com.example.chat.dm;

import com.example.chat.bans.UserBlockRepository;
import com.example.chat.dm.dto.*;
import com.example.chat.friendships.FriendshipRepository;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.chat.friendships.FriendshipService.lower;
import static com.example.chat.friendships.FriendshipService.higher;

@Service
public class DmService {

    private final DmThreadRepository dmThreadRepository;
    private final DmMessageRepository dmMessageRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository userBlockRepository;
    private final DmEventPublisher dmEventPublisher;

    public DmService(DmThreadRepository dmThreadRepository,
                     DmMessageRepository dmMessageRepository,
                     UserRepository userRepository,
                     FriendshipRepository friendshipRepository,
                     UserBlockRepository userBlockRepository,
                     DmEventPublisher dmEventPublisher) {
        this.dmThreadRepository = dmThreadRepository;
        this.dmMessageRepository = dmMessageRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.userBlockRepository = userBlockRepository;
        this.dmEventPublisher = dmEventPublisher;
    }

    @Transactional
    public DmThreadResponse getOrCreateThread(UUID requesterId, UUID targetId) {
        if (requesterId.equals(targetId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot DM yourself");
        User target = userRepository.findById(targetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        checkEligibility(requesterId, targetId);

        UUID u1 = lower(requesterId, targetId);
        UUID u2 = higher(requesterId, targetId);
        DmThread thread = dmThreadRepository.findByUser1IdAndUser2Id(u1, u2).orElseGet(() -> {
            DmThread t = new DmThread();
            t.setUser1Id(u1);
            t.setUser2Id(u2);
            return dmThreadRepository.save(t);
        });

        UUID otherId = thread.getUser1Id().equals(requesterId) ? thread.getUser2Id() : thread.getUser1Id();
        return new DmThreadResponse(thread.getId(), otherId, target.getUsername(), thread.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<DmThreadResponse> listThreads(UUID userId) {
        List<DmThread> threads = dmThreadRepository.findByUser1IdOrUser2Id(userId, userId);
        List<UUID> otherIds = threads.stream()
            .map(t -> t.getUser1Id().equals(userId) ? t.getUser2Id() : t.getUser1Id())
            .toList();
        Map<UUID, User> users = userRepository.findAllById(otherIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        return threads.stream().map(t -> {
            UUID otherId = t.getUser1Id().equals(userId) ? t.getUser2Id() : t.getUser1Id();
            User other = users.get(otherId);
            return new DmThreadResponse(t.getId(), otherId,
                other != null ? (other.getDeletedAt() != null ? "[deleted user]" : other.getUsername()) : "unknown", t.getCreatedAt());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<DmMessageResponse> getMessages(UUID requesterId, UUID threadId, UUID beforeId, int limit) {
        DmThread thread = findThreadOrThrow(threadId);
        verifyParticipant(thread, requesterId);

        List<DmMessage> messages;
        PageRequest page = PageRequest.of(0, Math.min(Math.max(1, limit), 100));
        if (beforeId != null) {
            DmMessage cursor = dmMessageRepository.findById(beforeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            messages = dmMessageRepository.findByThreadIdBefore(threadId, cursor.getCreatedAt(), page);
        } else {
            messages = dmMessageRepository.findByThreadIdOrderByCreatedAtDesc(threadId, page);
        }

        Map<UUID, User> users = userRepository.findAllById(
            messages.stream().map(DmMessage::getAuthorId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        return messages.stream().map(m -> toResponse(m, users)).toList();
    }

    @Transactional
    public DmMessageResponse sendMessage(UUID requesterId, UUID threadId, SendDmRequest req) {
        DmThread thread = findThreadOrThrow(threadId);
        verifyParticipant(thread, requesterId);
        UUID otherId = thread.getUser1Id().equals(requesterId) ? thread.getUser2Id() : thread.getUser1Id();
        checkEligibility(requesterId, otherId);

        User author = userRepository.findById(requesterId).orElseThrow();
        DmMessage msg = new DmMessage();
        msg.setThreadId(threadId);
        msg.setAuthorId(requesterId);
        msg.setContent(req.content());
        msg = dmMessageRepository.save(msg);

        DmMessageResponse response = new DmMessageResponse(
            msg.getId(), threadId, requesterId, author.getUsername(),
            msg.getContent(), msg.getCreatedAt(), null, false);
        dmEventPublisher.publishMessageNew(threadId, response);
        return response;
    }

    @Transactional
    public DmMessageResponse editMessage(UUID requesterId, UUID messageId, String content) {
        DmMessage msg = findMessageOrThrow(messageId);
        if (!msg.getAuthorId().equals(requesterId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (msg.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit deleted message");
        msg.setContent(content);
        msg.setEditedAt(OffsetDateTime.now());
        User author = userRepository.findById(requesterId).orElseThrow();
        msg = dmMessageRepository.save(msg);
        dmEventPublisher.publishMessageEdited(msg.getThreadId(), messageId, content, msg.getEditedAt());
        return toResponse(msg, Map.of(requesterId, author));
    }

    @Transactional
    public void deleteMessage(UUID requesterId, UUID messageId) {
        DmMessage msg = findMessageOrThrow(messageId);
        if (!msg.getAuthorId().equals(requesterId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        msg.setContent(null);
        msg.setDeletedAt(OffsetDateTime.now());
        dmMessageRepository.save(msg);
        dmEventPublisher.publishMessageDeleted(msg.getThreadId(), messageId);
    }

    private void checkEligibility(UUID a, UUID b) {
        if (!friendshipRepository.existsByUser1IdAndUser2Id(lower(a, b), higher(a, b)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not friends");
        if (userBlockRepository.existsByBlockerIdAndBlockedId(a, b) ||
            userBlockRepository.existsByBlockerIdAndBlockedId(b, a))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Blocked");
    }

    private void verifyParticipant(DmThread thread, UUID userId) {
        if (!thread.getUser1Id().equals(userId) && !thread.getUser2Id().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private DmThread findThreadOrThrow(UUID threadId) {
        return dmThreadRepository.findById(threadId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
    }

    private DmMessage findMessageOrThrow(UUID messageId) {
        return dmMessageRepository.findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
    }

    private DmMessageResponse toResponse(DmMessage m, Map<UUID, User> users) {
        User author = users.get(m.getAuthorId());
        return new DmMessageResponse(m.getId(), m.getThreadId(), m.getAuthorId(),
            author != null ? (author.getDeletedAt() != null ? "[deleted user]" : author.getUsername()) : "unknown",
            m.getDeletedAt() != null ? null : m.getContent(),
            m.getCreatedAt(), m.getEditedAt(), m.getDeletedAt() != null);
    }
}
