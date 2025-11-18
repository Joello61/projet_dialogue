package com.devops.projet_dialogue.repository;

import com.devops.projet_dialogue.model.Conversation;
import com.devops.projet_dialogue.model.Message;
import com.devops.projet_dialogue.model.Photo;
import com.devops.projet_dialogue.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour MessageRepository
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests du MessageRepository")
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User alice;
    private User bob;
    private Conversation conversation;
    private Photo photo1;
    private Photo photo2;

    @BeforeEach
    void setUp() {
        // Nettoyer la base
        messageRepository.deleteAll();

        // Créer les utilisateurs
        alice = new User("alice", "password1", "ROLE_USER");
        alice.setCreatedAt(LocalDateTime.now());

        bob = new User("bob", "password2", "ROLE_USER");
        bob.setCreatedAt(LocalDateTime.now());

        alice = entityManager.persistAndFlush(alice);
        bob = entityManager.persistAndFlush(bob);

        // Créer une conversation
        conversation = new Conversation();
        conversation.setUser1(alice);
        conversation.setUser2(bob);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation = entityManager.persistAndFlush(conversation);

        // Créer des photos
        photo1 = new Photo("uuid1.jpg", "photo1.jpg", "/uploads/uuid1.jpg", alice);
        photo1.setCreatedAt(LocalDateTime.now());
        photo1 = entityManager.persistAndFlush(photo1);

        photo2 = new Photo("uuid2.jpg", "photo2.jpg", "/uploads/uuid2.jpg", bob);
        photo2.setCreatedAt(LocalDateTime.now());
        photo2 = entityManager.persistAndFlush(photo2);
    }

    // ========== Tests findByConversationIdOrderByCreatedAtAsc ==========

    @Test
    @DisplayName("Devrait retourner les messages triés par date croissante")
    void shouldReturnMessages_OrderedByCreatedAtAsc() {
        // GIVEN - 3 messages à des moments différents
        Message msg1 = new Message();
        msg1.setConversation(conversation);
        msg1.setSender(alice);
        msg1.setText("Premier message");
        msg1.setCreatedAt(LocalDateTime.now().minusHours(3));

        Message msg2 = new Message();
        msg2.setConversation(conversation);
        msg2.setSender(bob);
        msg2.setText("Deuxième message");
        msg2.setCreatedAt(LocalDateTime.now().minusHours(2));

        Message msg3 = new Message();
        msg3.setConversation(conversation);
        msg3.setSender(alice);
        msg3.setText("Troisième message");
        msg3.setCreatedAt(LocalDateTime.now().minusHours(1));

        entityManager.persist(msg1);
        entityManager.persist(msg2);
        entityManager.persist(msg3);
        entityManager.flush();

        // WHEN
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(
                conversation.getId()
        );

        // THEN
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getText()).isEqualTo("Premier message");
        assertThat(messages.get(1).getText()).isEqualTo("Deuxième message");
        assertThat(messages.get(2).getText()).isEqualTo("Troisième message");

        // Vérifier l'ordre chronologique
        assertThat(messages.get(0).getCreatedAt())
                .isBefore(messages.get(1).getCreatedAt());
        assertThat(messages.get(1).getCreatedAt())
                .isBefore(messages.get(2).getCreatedAt());
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucun message")
    void shouldReturnEmptyList_WhenNoMessages() {
        // GIVEN - Conversation sans messages

        // WHEN
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(
                conversation.getId()
        );

        // THEN
        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("Devrait retourner seulement les messages de la conversation spécifiée")
    void shouldReturnOnlyMessagesFromSpecifiedConversation() {
        // GIVEN - Créer une 2ème conversation
        User charlie = new User("charlie", "password3", "ROLE_USER");
        charlie.setCreatedAt(LocalDateTime.now());
        charlie = entityManager.persistAndFlush(charlie);

        Conversation conversation2 = new Conversation();
        conversation2.setUser1(alice);
        conversation2.setUser2(charlie);
        conversation2.setCreatedAt(LocalDateTime.now());
        conversation2 = entityManager.persistAndFlush(conversation2);

        // Message dans conversation 1
        Message msg1 = new Message();
        msg1.setConversation(conversation);
        msg1.setSender(alice);
        msg1.setText("Message conversation 1");
        msg1.setCreatedAt(LocalDateTime.now());

        // Message dans conversation 2
        Message msg2 = new Message();
        msg2.setConversation(conversation2);
        msg2.setSender(alice);
        msg2.setText("Message conversation 2");
        msg2.setCreatedAt(LocalDateTime.now());

        entityManager.persist(msg1);
        entityManager.persist(msg2);
        entityManager.flush();

        // WHEN
        List<Message> conv1Messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(
                conversation.getId()
        );
        List<Message> conv2Messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(
                conversation2.getId()
        );

        // THEN
        assertThat(conv1Messages).hasSize(1);
        assertThat(conv1Messages.getFirst().getText()).isEqualTo("Message conversation 1");

        assertThat(conv2Messages).hasSize(1);
        assertThat(conv2Messages.getFirst().getText()).isEqualTo("Message conversation 2");
    }

    @Test
    @DisplayName("Devrait gérer les messages avec et sans photo")
    void shouldHandleMessages_WithAndWithoutPhotos() {
        // GIVEN
        Message msgWithPhoto = new Message();
        msgWithPhoto.setConversation(conversation);
        msgWithPhoto.setSender(alice);
        msgWithPhoto.setText("Message avec photo");
        msgWithPhoto.setPhoto(photo1);
        msgWithPhoto.setCreatedAt(LocalDateTime.now().minusHours(2));

        Message msgWithoutPhoto = new Message();
        msgWithoutPhoto.setConversation(conversation);
        msgWithoutPhoto.setSender(bob);
        msgWithoutPhoto.setText("Message sans photo");
        msgWithoutPhoto.setPhoto(null);
        msgWithoutPhoto.setCreatedAt(LocalDateTime.now().minusHours(1));

        entityManager.persist(msgWithPhoto);
        entityManager.persist(msgWithoutPhoto);
        entityManager.flush();

        // WHEN
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(
                conversation.getId()
        );

        // THEN
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getPhoto()).isNotNull();
        assertThat(messages.get(1).getPhoto()).isNull();
    }

    // ========== Tests findPhotosInConversation ==========

    @Test
    @DisplayName("Devrait retourner toutes les photos d'une conversation")
    void shouldReturnAllPhotosInConversation() {
        // GIVEN - 2 messages avec photos, 1 sans photo
        Message msg1 = new Message();
        msg1.setConversation(conversation);
        msg1.setSender(alice);
        msg1.setText("Première photo");
        msg1.setPhoto(photo1);
        msg1.setCreatedAt(LocalDateTime.now().minusHours(3));

        Message msg2 = new Message();
        msg2.setConversation(conversation);
        msg2.setSender(bob);
        msg2.setText("Texte seulement");
        msg2.setPhoto(null);
        msg2.setCreatedAt(LocalDateTime.now().minusHours(2));

        Message msg3 = new Message();
        msg3.setConversation(conversation);
        msg3.setSender(alice);
        msg3.setText("Deuxième photo");
        msg3.setPhoto(photo2);
        msg3.setCreatedAt(LocalDateTime.now().minusHours(1));

        entityManager.persist(msg1);
        entityManager.persist(msg2);
        entityManager.persist(msg3);
        entityManager.flush();

        // WHEN
        List<Photo> photos = messageRepository.findPhotosInConversation(conversation.getId());

        // THEN
        assertThat(photos).hasSize(2);
        assertThat(photos).extracting(Photo::getFilename)
                .containsExactly("uuid1.jpg", "uuid2.jpg"); // Ordre chronologique
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucune photo")
    void shouldReturnEmptyList_WhenNoPhotos() {
        // GIVEN - Message sans photo
        Message msg = new Message();
        msg.setConversation(conversation);
        msg.setSender(alice);
        msg.setText("Texte seulement");
        msg.setPhoto(null);
        msg.setCreatedAt(LocalDateTime.now());
        entityManager.persistAndFlush(msg);

        // WHEN
        List<Photo> photos = messageRepository.findPhotosInConversation(conversation.getId());

        // THEN
        assertThat(photos).isEmpty();
    }

    @Test
    @DisplayName("Devrait trier les photos par date croissante")
    void shouldSortPhotos_ByCreatedAtAsc() {
        // GIVEN - 3 photos à des moments différents
        Photo oldPhoto = new Photo("old.jpg", "old.jpg", "/uploads/old.jpg", alice);
        oldPhoto.setCreatedAt(LocalDateTime.now().minusDays(3));
        oldPhoto = entityManager.persistAndFlush(oldPhoto);

        Photo mediumPhoto = new Photo("medium.jpg", "medium.jpg", "/uploads/medium.jpg", bob);
        mediumPhoto.setCreatedAt(LocalDateTime.now().minusDays(2));
        mediumPhoto = entityManager.persistAndFlush(mediumPhoto);

        Photo recentPhoto = new Photo("recent.jpg", "recent.jpg", "/uploads/recent.jpg", alice);
        recentPhoto.setCreatedAt(LocalDateTime.now().minusDays(1));
        recentPhoto = entityManager.persistAndFlush(recentPhoto);

        // Messages avec ces photos (dans le désordre)
        Message msg1 = new Message();
        msg1.setConversation(conversation);
        msg1.setSender(alice);
        msg1.setPhoto(recentPhoto);
        msg1.setCreatedAt(LocalDateTime.now().minusDays(1));

        Message msg2 = new Message();
        msg2.setConversation(conversation);
        msg2.setSender(bob);
        msg2.setPhoto(oldPhoto);
        msg2.setCreatedAt(LocalDateTime.now().minusDays(3));

        Message msg3 = new Message();
        msg3.setConversation(conversation);
        msg3.setSender(alice);
        msg3.setPhoto(mediumPhoto);
        msg3.setCreatedAt(LocalDateTime.now().minusDays(2));

        entityManager.persist(msg1);
        entityManager.persist(msg2);
        entityManager.persist(msg3);
        entityManager.flush();

        // WHEN
        List<Photo> photos = messageRepository.findPhotosInConversation(conversation.getId());

        // THEN - Ordre: old -> medium -> recent
        assertThat(photos).hasSize(3);
        assertThat(photos.get(0).getFilename()).isEqualTo("old.jpg");
        assertThat(photos.get(1).getFilename()).isEqualTo("medium.jpg");
        assertThat(photos.get(2).getFilename()).isEqualTo("recent.jpg");
    }

    @Test
    @DisplayName("Devrait isoler les photos par conversation")
    void shouldIsolatePhotos_ByConversation() {
        // GIVEN - 2 conversations avec photos
        User charlie = new User("charlie", "password3", "ROLE_USER");
        charlie.setCreatedAt(LocalDateTime.now());
        charlie = entityManager.persistAndFlush(charlie);

        Conversation conversation2 = new Conversation();
        conversation2.setUser1(alice);
        conversation2.setUser2(charlie);
        conversation2.setCreatedAt(LocalDateTime.now());
        conversation2 = entityManager.persistAndFlush(conversation2);

        // Photo dans conversation 1
        Message msg1 = new Message();
        msg1.setConversation(conversation);
        msg1.setSender(alice);
        msg1.setPhoto(photo1);
        msg1.setCreatedAt(LocalDateTime.now());

        // Photo dans conversation 2
        Message msg2 = new Message();
        msg2.setConversation(conversation2);
        msg2.setSender(alice);
        msg2.setPhoto(photo2);
        msg2.setCreatedAt(LocalDateTime.now());

        entityManager.persist(msg1);
        entityManager.persist(msg2);
        entityManager.flush();

        // WHEN
        List<Photo> photosConv1 = messageRepository.findPhotosInConversation(conversation.getId());
        List<Photo> photosConv2 = messageRepository.findPhotosInConversation(conversation2.getId());

        // THEN
        assertThat(photosConv1).hasSize(1);
        assertThat(photosConv1.getFirst()).isEqualTo(photo1);

        assertThat(photosConv2).hasSize(1);
        assertThat(photosConv2.getFirst()).isEqualTo(photo2);
    }

    // ========== Tests save ==========

    @Test
    @DisplayName("Devrait sauvegarder un nouveau message")
    void shouldSaveNewMessage() {
        // GIVEN
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(alice);
        message.setText("Nouveau message");
        message.setCreatedAt(LocalDateTime.now());

        // WHEN
        Message saved = messageRepository.save(message);

        // THEN
        assertThat(saved.getId()).isNotNull();
        assertThat(messageRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("Devrait sauvegarder un message avec photo")
    void shouldSaveMessage_WithPhoto() {
        // GIVEN
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(alice);
        message.setText("Avec photo");
        message.setPhoto(photo1);
        message.setCreatedAt(LocalDateTime.now());

        // WHEN
        Message saved = messageRepository.save(message);

        // THEN
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPhoto()).isEqualTo(photo1);
    }

    @Test
    @DisplayName("Devrait compter le nombre de messages")
    void shouldCountMessages() {
        // GIVEN - 2 messages
        Message msg1 = new Message();
        msg1.setConversation(conversation);
        msg1.setSender(alice);
        msg1.setText("Message 1");
        msg1.setCreatedAt(LocalDateTime.now());

        Message msg2 = new Message();
        msg2.setConversation(conversation);
        msg2.setSender(bob);
        msg2.setText("Message 2");
        msg2.setCreatedAt(LocalDateTime.now());

        entityManager.persist(msg1);
        entityManager.persist(msg2);
        entityManager.flush();

        // WHEN
        long count = messageRepository.count();

        // THEN
        assertThat(count).isEqualTo(2);
    }
}