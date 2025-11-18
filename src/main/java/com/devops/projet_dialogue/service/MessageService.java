package com.devops.projet_dialogue.service;

import com.devops.projet_dialogue.model.*;
import com.devops.projet_dialogue.repository.MessageRepository;
import com.devops.projet_dialogue.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    public Message sendMessage(Long conversationId, User sender, String text, Photo photo) {

        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));

        Message msg = new Message();
        msg.setConversation(conv);
        msg.setSender(sender);
        msg.setText(text);
        msg.setPhoto(photo);
        msg.setCreatedAt(LocalDateTime.now());

        return messageRepository.save(msg);
    }

    public List<Message> listMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public List<Photo> listPhotos(Long conversationId) {
        return messageRepository.findPhotosInConversation(conversationId);
    }
}
