package com.devops.projet_dialogue.service;

import com.devops.projet_dialogue.model.Conversation;
import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.repository.ConversationRepository;
import com.devops.projet_dialogue.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    public Conversation getOrCreate(Long userAId, Long userBId) {

        // Vérifier si la conversation existe déjà
        return conversationRepository.findByUsers(userAId, userBId)
                .orElseGet(() -> {
                    User userA = userRepository.findById(userAId)
                            .orElseThrow();
                    User userB = userRepository.findById(userBId)
                            .orElseThrow();

                    Conversation c = new Conversation();
                    c.setUser1(userA);
                    c.setUser2(userB);
                    c.setCreatedAt(LocalDateTime.now());

                    return conversationRepository.save(c);
                });
    }

    public List<Conversation> findAllForUser(Long userId) {
        return conversationRepository.findAllForUser(userId);
    }

    public Conversation findById(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
    }
}
