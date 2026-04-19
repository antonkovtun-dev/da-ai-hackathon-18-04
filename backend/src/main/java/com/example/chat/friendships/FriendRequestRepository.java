package com.example.chat.friendships;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    Optional<FriendRequest> findBySenderIdAndReceiverId(UUID senderId, UUID receiverId);

    List<FriendRequest> findByReceiverIdAndStatus(UUID receiverId, FriendRequestStatus status);

    List<FriendRequest> findBySenderIdAndStatus(UUID senderId, FriendRequestStatus status);

    @Query("SELECT COUNT(r) FROM FriendRequest r WHERE r.status = :status " +
           "AND ((r.senderId = :a AND r.receiverId = :b) OR (r.senderId = :b AND r.receiverId = :a))")
    long countPendingBetween(@Param("a") UUID a, @Param("b") UUID b,
                             @Param("status") FriendRequestStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE FriendRequest r SET r.status = :canceled, r.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE r.status = :pending " +
           "AND ((r.senderId = :a AND r.receiverId = :b) OR (r.senderId = :b AND r.receiverId = :a))")
    void cancelPendingBetween(@Param("a") UUID a, @Param("b") UUID b,
                              @Param("pending") FriendRequestStatus pending,
                              @Param("canceled") FriendRequestStatus canceled);

    @Modifying
    @Transactional
    @Query("DELETE FROM FriendRequest r WHERE r.senderId = :userId OR r.receiverId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
