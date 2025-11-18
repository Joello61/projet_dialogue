package com.devops.projet_dialogue.service;

import com.devops.projet_dialogue.model.Conversation;
import com.devops.projet_dialogue.model.Message;
import com.devops.projet_dialogue.model.Photo;
import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.repository.ConversationRepository;
import com.devops.projet_dialogue.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour MessageService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du MessageService")
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private MessageService messageService;

    private User alice;
    private User bob;
    private Conversation conversation;
    private Photo testPhoto;

    @BeforeEach
    void setUp() {
        // Préparation des données de test
        alice = new User("alice", "password1", "ROLE_USER");
        alice.setCreatedAt(LocalDateTime.now());

        bob = new User("bob", "password2", "ROLE_USER");
        bob.setCreatedAt(LocalDateTime.now());

        conversation = new Conversation();
        conversation.setUser1(alice);
        conversation.setUser2(bob);
        conversation.setCreatedAt(LocalDateTime.now());

        testPhoto = new Photo(
                "uuid_photo.jpg",
                "photo.jpg",
                "/uploads/uuid_photo.jpg",
                alice
        );
    }

    // ========== Tests sendMessage ==========

    @Test
    @DisplayName("Devrait envoyer un message texte simple")
    void shouldSendTextMessage_Successfully() {
        // GIVEN
        Long conversationId = 1L;
        String messageText = "Bonjour Bob !";

        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(conversation));

        Message savedMessage = new Message();
        savedMessage.setConversation(conversation);
        savedMessage.setSender(alice);
        savedMessage.setText(messageText);
        savedMessage.setCreatedAt(LocalDateTime.now());

        when(messageRepository.save(any(Message.class)))
                .thenReturn(savedMessage);

        // WHEN
        Message result = messageService.sendMessage(conversationId, alice, messageText, null);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getText()).isEqualTo(messageText);
        assertThat(result.getSender()).isEqualTo(alice);
        assertThat(result.getConversation()).isEqualTo(conversation);
        assertThat(result.getPhoto()).isNull();

        verify(conversationRepository, times(1)).findById(conversationId);
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    @DisplayName("Devrait envoyer un message avec photo")
    void shouldSendMessageWithPhoto_Successfully() {
        // GIVEN
        Long conversationId = 1L;
        String messageText = "Regarde cette photo !";

        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(conversation));

        Message savedMessage = new Message();
        savedMessage.setConversation(conversation);
        savedMessage.setSender(alice);
        savedMessage.setText(messageText);
        savedMessage.setPhoto(testPhoto);
        savedMessage.setCreatedAt(LocalDateTime.now());

        when(messageRepository.save(any(Message.class)))
                .thenReturn(savedMessage);

        // WHEN
        Message result = messageService.sendMessage(conversationId, alice, messageText, testPhoto);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getText()).isEqualTo(messageText);
        assertThat(result.getPhoto()).isEqualTo(testPhoto);
        assertThat(result.getSender()).isEqualTo(alice);

        verify(conversationRepository, times(1)).findById(conversationId);
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    @DisplayName("Devrait envoyer une photo sans texte")
    void shouldSendPhotoOnly_Successfully() {
        // GIVEN
        Long conversationId = 1L;

        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(conversation));

        Message savedMessage = new Message();
        savedMessage.setConversation(conversation);
        savedMessage.setSender(bob);
        savedMessage.setText(null);
        savedMessage.setPhoto(testPhoto);
        savedMessage.setCreatedAt(LocalDateTime.now());

        when(messageRepository.save(any(Message.class)))
                .thenReturn(savedMessage);

        // WHEN
        Message result = messageService.sendMessage(conversationId, bob, null, testPhoto);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getText()).isNull();
        assertThat(result.getPhoto()).isEqualTo(testPhoto);
        assertThat(result.getSender()).isEqualTo(bob);

        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    @DisplayName("Devrait lever une exception si conversation non trouvée")
    void shouldThrowException_WhenConversationNotFound() {
        // GIVEN
        Long conversationId = 999L;
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() ->
                messageService.sendMessage(conversationId, alice, "Test", null)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Conversation non trouvée");

        verify(conversationRepository, times(1)).findById(conversationId);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait définir la date de création automatiquement")
    void shouldSetCreatedAtAutomatically() {
        // GIVEN
        Long conversationId = 1L;
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        LocalDateTime beforeSend = LocalDateTime.now();
        messageService.sendMessage(conversationId, alice, "Test", null);
        LocalDateTime afterSend = LocalDateTime.now();

        // THEN
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());

        Message capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getCreatedAt()).isNotNull();
        assertThat(capturedMessage.getCreatedAt())
                .isAfterOrEqualTo(beforeSend)
                .isBeforeOrEqualTo(afterSend);
    }

    // ========== Tests listMessages ==========

    @Test
    @DisplayName("Devrait lister tous les messages d'une conversation")
    void shouldListAllMessagesInConversation() {
        // GIVEN
        Long conversationId = 1L;

        Message msg1 = new Message();
        msg1.setConversation(conversation);
        msg1.setSender(alice);
        msg1.setText("Premier message");
        msg1.setCreatedAt(LocalDateTime.now().minusHours(2));

        Message msg2 = new Message();
        msg2.setConversation(conversation);
        msg2.setSender(bob);
        msg2.setText("Deuxième message");
        msg2.setCreatedAt(LocalDateTime.now().minusHours(1));

        Message msg3 = new Message();
        msg3.setConversation(conversation);
        msg3.setSender(alice);
        msg3.setText("Troisième message");
        msg3.setCreatedAt(LocalDateTime.now());

        List<Message> messages = Arrays.asList(msg1, msg2, msg3);

        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(messages);

        // WHEN
        List<Message> result = messageService.listMessages(conversationId);

        // THEN
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(msg1, msg2, msg3);
        assertThat(result.get(0).getText()).isEqualTo("Premier message");
        assertThat(result.get(2).getText()).isEqualTo("Troisième message");

        verify(messageRepository, times(1))
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucun message")
    void shouldReturnEmptyList_WhenNoMessages() {
        // GIVEN
        Long conversationId = 1L;
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of());

        // WHEN
        List<Message> result = messageService.listMessages(conversationId);

        // THEN
        assertThat(result).isEmpty();
        verify(messageRepository, times(1))
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    // ========== Tests listPhotos ==========

    @Test
    @DisplayName("Devrait lister toutes les photos d'une conversation")
    void shouldListAllPhotosInConversation() {
        // GIVEN
        Long conversationId = 1L;

        Photo photo1 = new Photo("uuid1.jpg", "photo1.jpg", "/uploads/uuid1.jpg", alice);
        Photo photo2 = new Photo("uuid2.jpg", "photo2.jpg", "/uploads/uuid2.jpg", bob);
        Photo photo3 = new Photo("uuid3.jpg", "photo3.jpg", "/uploads/uuid3.jpg", alice);

        List<Photo> photos = Arrays.asList(photo1, photo2, photo3);

        when(messageRepository.findPhotosInConversation(conversationId))
                .thenReturn(photos);

        // WHEN
        List<Photo> result = messageService.listPhotos(conversationId);

        // THEN
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(photo1, photo2, photo3);
        assertThat(result.get(0).getAuthor()).isEqualTo(alice);
        assertThat(result.get(1).getAuthor()).isEqualTo(bob);

        verify(messageRepository, times(1))
                .findPhotosInConversation(conversationId);
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucune photo")
    void shouldReturnEmptyList_WhenNoPhotos() {
        // GIVEN
        Long conversationId = 1L;
        when(messageRepository.findPhotosInConversation(conversationId))
                .thenReturn(List.of());

        // WHEN
        List<Photo> result = messageService.listPhotos(conversationId);

        // THEN
        assertThat(result).isEmpty();
        verify(messageRepository, times(1))
                .findPhotosInConversation(conversationId);
    }

    @Test
    @DisplayName("Devrait gérer les messages avec et sans photos")
    void shouldHandleMessagesWithAndWithoutPhotos() {
        // GIVEN
        Long conversationId = 1L;

        Message msgWithPhoto = new Message();
        msgWithPhoto.setText("Avec photo");
        msgWithPhoto.setPhoto(testPhoto);

        Message msgWithoutPhoto = new Message();
        msgWithoutPhoto.setText("Sans photo");
        msgWithoutPhoto.setPhoto(null);

        List<Message> messages = Arrays.asList(msgWithPhoto, msgWithoutPhoto);

        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(messages);

        // WHEN
        List<Message> result = messageService.listMessages(conversationId);

        // THEN
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPhoto()).isNotNull();
        assertThat(result.get(1).getPhoto()).isNull();
    }
}