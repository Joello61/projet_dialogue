package com.devops.projet_dialogue.repository;


import com.devops.projet_dialogue.model.Message;
import com.devops.projet_dialogue.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("""
        SELECT m.photo FROM Message m
        WHERE m.conversation.id = :conversationId
          AND m.photo IS NOT NULL
        ORDER BY m.createdAt ASC
    """)
    List<Photo> findPhotosInConversation(Long conversationId);
}
