package com.example.chat.attachments;

import com.example.chat.attachments.dto.AttachmentResponse;
import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.messages.Message;
import com.example.chat.messages.MessageRepository;
import com.example.chat.messages.dto.MessageResponse;
import com.example.chat.readstate.ReadStateService;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import com.example.chat.websocket.RoomEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final MessageRepository messageRepository;
    private final RoomMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoomEventPublisher eventPublisher;
    private final ReadStateService readStateService;
    private final Path uploadDir;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            MessageRepository messageRepository,
            RoomMembershipRepository membershipRepository,
            UserRepository userRepository,
            RoomEventPublisher eventPublisher,
            ReadStateService readStateService,
            @Value("${app.upload-dir:/tmp/chat-uploads}") String uploadDirPath) {
        this.attachmentRepository = attachmentRepository;
        this.messageRepository = messageRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.readStateService = readStateService;
        this.uploadDir = Path.of(uploadDirPath);
    }

    @Transactional
    public MessageResponse upload(UUID roomId, UUID uploaderId, MultipartFile file, String comment) {
        membershipRepository.findByRoomIdAndUserId(roomId, uploaderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));

        if (file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");

        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        boolean isImage = contentType.startsWith("image/");
        long maxBytes = isImage ? 3L * 1024 * 1024 : 20L * 1024 * 1024;
        if (file.getSize() > maxBytes)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    isImage ? "Image exceeds 3 MB limit" : "File exceeds 20 MB limit");

        // Create message
        Message msg = new Message();
        msg.setRoomId(roomId);
        msg.setAuthorId(uploaderId);
        String trimmed = (comment != null) ? comment.strip() : "";
        msg.setContent(trimmed.isEmpty() ? null : trimmed);
        msg = messageRepository.save(msg);

        // Store file — UUID filename prevents path traversal
        String storedName = UUID.randomUUID().toString();
        try {
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), uploadDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File storage failed");
        }

        // Save attachment record
        Attachment att = new Attachment();
        att.setMessageId(msg.getId());
        att.setRoomId(roomId);
        att.setUploaderId(uploaderId);
        att.setFilename(sanitize(file.getOriginalFilename()));
        att.setContentType(contentType);
        att.setSize(file.getSize());
        att.setStoredPath(storedName);
        att = attachmentRepository.save(att);

        readStateService.markRead(roomId, uploaderId);

        User uploader = userRepository.findById(uploaderId).orElseThrow();
        AttachmentResponse attResp = new AttachmentResponse(
                att.getId(), att.getFilename(), att.getContentType(), att.getSize());
        MessageResponse resp = new MessageResponse(
                msg.getId(), msg.getRoomId(), msg.getAuthorId(), uploader.getUsername(),
                msg.getContent(), msg.getCreatedAt(), null, false, attResp);
        eventPublisher.publishMessageNew(roomId, resp);
        return resp;
    }

    public ResponseEntity<Resource> download(UUID attachmentId, UUID requesterId) {
        Attachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!membershipRepository.existsByRoomIdAndUserId(att.getRoomId(), requesterId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        Path filePath = uploadDir.resolve(att.getStoredPath()).normalize();
        if (!filePath.startsWith(uploadDir.normalize()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File missing on disk");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + att.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(att.getContentType()))
                .body(resource);
    }

    private String sanitize(String original) {
        if (original == null) return "file";
        return original.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
