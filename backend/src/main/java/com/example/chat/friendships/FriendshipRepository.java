package com.example.chat.friendships;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    boolean existsByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);

    List<Friendship> findByUser1IdOrUser2Id(UUID user1Id, UUID user2Id);

    @Modifying
    @Transactional
    @Query("DELETE FROM Friendship f WHERE " +
           "(f.user1Id = :a AND f.user2Id = :b) OR (f.user1Id = :b AND f.user2Id = :a)")
    void deleteBetween(@Param("a") UUID a, @Param("b") UUID b);

    @Modifying
    @Transactional
    @Query("DELETE FROM Friendship f WHERE f.user1Id = :userId OR f.user2Id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
