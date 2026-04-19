package com.example.chat.attachments;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    Optional<Attachment> findByMessageId(UUID messageId);
    List<Attachment> findByMessageIdIn(Collection<UUID> messageIds);
}
