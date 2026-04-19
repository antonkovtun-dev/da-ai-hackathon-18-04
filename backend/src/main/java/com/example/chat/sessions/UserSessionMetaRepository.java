package com.example.chat.sessions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

public interface UserSessionMetaRepository extends JpaRepository<UserSessionMeta, String> {
    @Transactional
    void deleteByUserId(UUID userId);
}
