package com.example.chat.sessions;

import com.example.chat.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    public SessionService(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    public List<SessionInfo> listSessions(UUID userId, String currentSessionId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return jdbcTemplate.query(
                "SELECT s.session_id, s.creation_time, s.last_access_time, " +
                "m.user_agent, m.ip_address " +
                "FROM spring_session s " +
                "LEFT JOIN user_session_meta m ON s.session_id = m.session_id " +
                "WHERE s.principal_name = ? " +
                "ORDER BY s.last_access_time DESC",
                (rs, i) -> new SessionInfo(
                        rs.getString("session_id"),
                        Instant.ofEpochMilli(rs.getLong("creation_time")),
                        Instant.ofEpochMilli(rs.getLong("last_access_time")),
                        rs.getString("user_agent"),
                        rs.getString("ip_address"),
                        rs.getString("session_id").equals(currentSessionId)
                ),
                user.getEmail()
        );
    }

    @Transactional
    public void revokeSession(UUID userId, String sessionId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        int deleted = jdbcTemplate.update(
                "DELETE FROM spring_session WHERE session_id = ? AND principal_name = ?",
                sessionId, user.getEmail());
        if (deleted == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        jdbcTemplate.update("DELETE FROM user_session_meta WHERE session_id = ?", sessionId);
    }

    public record SessionInfo(
            String sessionId,
            Instant createdAt,
            Instant lastActiveAt,
            String userAgent,
            String ipAddress,
            boolean current
    ) {}
}
