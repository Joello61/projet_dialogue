package com.devops.projet_dialogue.repository;

import com.devops.projet_dialogue.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
        SELECT c FROM Conversation c 
        WHERE (c.user1.id = :userA AND c.user2.id = :userB)
           OR (c.user1.id = :userB AND c.user2.id = :userA)
    """)
    Optional<Conversation> findByUsers(Long userA, Long userB);

    @Query("""
        SELECT c FROM Conversation c
        WHERE c.user1.id = :userId OR c.user2.id = :userId
        ORDER BY c.createdAt DESC
    """)
    List<Conversation> findAllForUser(Long userId);
}
