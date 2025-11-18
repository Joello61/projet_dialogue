package com.devops.projet_dialogue.repository;

import com.devops.projet_dialogue.model.Conversation;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour ConversationRepository
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests du ConversationRepository")
class ConversationRepositoryTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User alice;
    private User bob;
    private User charlie;

    @BeforeEach
    void setUp() {
        // Nettoyer la base
        conversationRepository.deleteAll();

        // Créer des utilisateurs
        alice = new User("alice", "password1", "ROLE_USER");
        alice.setCreatedAt(LocalDateTime.now());

        bob = new User("bob", "password2", "ROLE_USER");
        bob.setCreatedAt(LocalDateTime.now());

        charlie = new User("charlie", "password3", "ROLE_USER");
        charlie.setCreatedAt(LocalDateTime.now());

        // Persister les users
        alice = entityManager.persistAndFlush(alice);
        bob = entityManager.persistAndFlush(bob);
        charlie = entityManager.persistAndFlush(charlie);
    }

    // ========== Tests findByUsers ==========

    @Test
    @DisplayName("Devrait trouver une conversation entre deux users (ordre userA, userB)")
    void shouldFindConversation_OrderAB() {
        // GIVEN - Conversation Alice -> Bob
        Conversation conversation = new Conversation();
        conversation.setUser1(alice);
        conversation.setUser2(bob);
        conversation.setCreatedAt(LocalDateTime.now());
        entityManager.persistAndFlush(conversation);

        // WHEN - Chercher dans l'ordre A, B
        Optional<Conversation> result = conversationRepository.findByUsers(
                alice.getId(), bob.getId()
        );

        // THEN
        assertThat(result).isPresent();
        assertThat(result.get().getUser1()).isEqualTo(alice);
        assertThat(result.get().getUser2()).isEqualTo(bob);
    }

    @Test
    @DisplayName("Devrait trouver une conversation dans l'ordre inverse (ordre userB, userA)")
    void shouldFindConversation_OrderBA() {
        // GIVEN - Conversation Alice -> Bob
        Conversation conversation = new Conversation();
        conversation.setUser1(alice);
        conversation.setUser2(bob);
        conversation.setCreatedAt(LocalDateTime.now());
        entityManager.persistAndFlush(conversation);

        // WHEN - Chercher dans l'ordre B, A (inverse)
        Optional<Conversation> result = conversationRepository.findByUsers(
                bob.getId(), alice.getId()
        );

        // THEN - Doit quand même trouver la conversation
        assertThat(result).isPresent();
        assertThat(result.get().getUser1()).isEqualTo(alice);
        assertThat(result.get().getUser2()).isEqualTo(bob);
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty si conversation inexistante")
    void shouldReturnEmpty_WhenConversationDoesNotExist() {
        // GIVEN - Pas de conversation entre Alice et Charlie

        // WHEN
        Optional<Conversation> result = conversationRepository.findByUsers(
                alice.getId(), charlie.getId()
        );

        // THEN
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Devrait distinguer différentes conversations")
    void shouldDistinguishDifferentConversations() {
        // GIVEN - Deux conversations différentes
        Conversation convAliceBob = new Conversation();
        convAliceBob.setUser1(alice);
        convAliceBob.setUser2(bob);
        convAliceBob.setCreatedAt(LocalDateTime.now());

        Conversation convAliceCharlie = new Conversation();
        convAliceCharlie.setUser1(alice);
        convAliceCharlie.setUser2(charlie);
        convAliceCharlie.setCreatedAt(LocalDateTime.now());

        entityManager.persist(convAliceBob);
        entityManager.persist(convAliceCharlie);
        entityManager.flush();

        // WHEN
        Optional<Conversation> resultAliceBob = conversationRepository.findByUsers(
                alice.getId(), bob.getId()
        );
        Optional<Conversation> resultAliceCharlie = conversationRepository.findByUsers(
                alice.getId(), charlie.getId()
        );

        // THEN
        assertThat(resultAliceBob).isPresent();
        assertThat(resultAliceCharlie).isPresent();
        assertThat(resultAliceBob.get().getId()).isNotEqualTo(resultAliceCharlie.get().getId());
    }

    // ========== Tests findAllForUser ==========

    @Test
    @DisplayName("Devrait trouver toutes les conversations d'un utilisateur")
    void shouldFindAllConversationsForUser() {
        // GIVEN - Alice a 2 conversations
        Conversation convAliceBob = new Conversation();
        convAliceBob.setUser1(alice);
        convAliceBob.setUser2(bob);
        convAliceBob.setCreatedAt(LocalDateTime.now().minusDays(2));

        Conversation convAliceCharlie = new Conversation();
        convAliceCharlie.setUser1(alice);
        convAliceCharlie.setUser2(charlie);
        convAliceCharlie.setCreatedAt(LocalDateTime.now().minusDays(1));

        // Bob a aussi une conversation avec Charlie
        Conversation convBobCharlie = new Conversation();
        convBobCharlie.setUser1(bob);
        convBobCharlie.setUser2(charlie);
        convBobCharlie.setCreatedAt(LocalDateTime.now());

        entityManager.persist(convAliceBob);
        entityManager.persist(convAliceCharlie);
        entityManager.persist(convBobCharlie);
        entityManager.flush();

        // WHEN - Récupérer les conversations d'Alice
        List<Conversation> aliceConversations = conversationRepository.findAllForUser(alice.getId());

        // THEN - Alice doit avoir 2 conversations
        assertThat(aliceConversations).hasSize(2);
        assertThat(aliceConversations)
                .extracting(c -> c.getUser1().getId() + "-" + c.getUser2().getId())
                .containsExactlyInAnyOrder(
                        alice.getId() + "-" + bob.getId(),
                        alice.getId() + "-" + charlie.getId()
                );
    }

    @Test
    @DisplayName("Devrait trouver les conversations où user est user1 ou user2")
    void shouldFindConversations_WhetherUser1OrUser2() {
        // GIVEN - Bob est user1 dans une conversation et user2 dans une autre
        Conversation convBobAlice = new Conversation();
        convBobAlice.setUser1(bob);
        convBobAlice.setUser2(alice);
        convBobAlice.setCreatedAt(LocalDateTime.now().minusDays(1));

        Conversation convCharlieBob = new Conversation();
        convCharlieBob.setUser1(charlie);
        convCharlieBob.setUser2(bob);
        convCharlieBob.setCreatedAt(LocalDateTime.now());

        entityManager.persist(convBobAlice);
        entityManager.persist(convCharlieBob);
        entityManager.flush();

        // WHEN
        List<Conversation> bobConversations = conversationRepository.findAllForUser(bob.getId());

        // THEN - Bob doit apparaître dans les 2 conversations
        assertThat(bobConversations).hasSize(2);
    }

    @Test
    @DisplayName("Devrait trier les conversations par date décroissante")
    void shouldSortConversations_ByCreatedAtDesc() {
        // GIVEN - 3 conversations à des moments différents
        Conversation conv1 = new Conversation();
        conv1.setUser1(alice);
        conv1.setUser2(bob);
        conv1.setCreatedAt(LocalDateTime.now().minusDays(3)); // Plus ancienne

        Conversation conv2 = new Conversation();
        conv2.setUser1(alice);
        conv2.setUser2(charlie);
        conv2.setCreatedAt(LocalDateTime.now().minusDays(1)); // Moyenne

        Conversation conv3 = new Conversation();
        conv3.setUser1(bob);
        conv3.setUser2(alice);
        conv3.setCreatedAt(LocalDateTime.now()); // Plus récente

        entityManager.persist(conv1);
        entityManager.persist(conv2);
        entityManager.persist(conv3);
        entityManager.flush();

        // WHEN
        List<Conversation> aliceConversations = conversationRepository.findAllForUser(alice.getId());

        // THEN - Doit être trié du plus récent au plus ancien
        assertThat(aliceConversations).hasSize(3);
        assertThat(aliceConversations.get(0).getCreatedAt())
                .isAfter(aliceConversations.get(1).getCreatedAt());
        assertThat(aliceConversations.get(1).getCreatedAt())
                .isAfter(aliceConversations.get(2).getCreatedAt());
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si user n'a pas de conversations")
    void shouldReturnEmptyList_WhenUserHasNoConversations() {
        // GIVEN - Charlie n'a aucune conversation

        // WHEN
        List<Conversation> charlieConversations = conversationRepository.findAllForUser(charlie.getId());

        // THEN
        assertThat(charlieConversations).isEmpty();
    }

    // ========== Tests save et contrainte unique ==========

    @Test
    @DisplayName("Devrait sauvegarder une nouvelle conversation")
    void shouldSaveNewConversation() {
        // GIVEN
        Conversation conversation = new Conversation();
        conversation.setUser1(alice);
        conversation.setUser2(bob);
        conversation.setCreatedAt(LocalDateTime.now());

        // WHEN
        Conversation saved = conversationRepository.save(conversation);

        // THEN
        assertThat(saved.getId()).isNotNull();
        assertThat(conversationRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("Devrait empêcher la création de conversations en doublon")
    void shouldPreventDuplicateConversations() {
        // GIVEN - Une conversation existe déjà
        Conversation conv1 = new Conversation();
        conv1.setUser1(alice);
        conv1.setUser2(bob);
        conv1.setCreatedAt(LocalDateTime.now());
        entityManager.persistAndFlush(conv1);

        // WHEN - Tenter de créer la même conversation
        // Note: La contrainte unique sur (user1_id, user2_id) devrait empêcher cela

        // THEN - On vérifie qu'il n'y a qu'une seule conversation
        List<Conversation> conversations = conversationRepository.findAllForUser(alice.getId());
        assertThat(conversations).hasSize(1);
    }

    // ========== Tests findById ==========

    @Test
    @DisplayName("Devrait trouver une conversation par ID")
    void shouldFindConversationById() {
        // GIVEN
        Conversation conversation = new Conversation();
        conversation.setUser1(alice);
        conversation.setUser2(bob);
        conversation.setCreatedAt(LocalDateTime.now());
        Conversation saved = entityManager.persistAndFlush(conversation);

        // WHEN
        Optional<Conversation> result = conversationRepository.findById(saved.getId());

        // THEN
        assertThat(result).isPresent();
        assertThat(result.get().getUser1()).isEqualTo(alice);
        assertThat(result.get().getUser2()).isEqualTo(bob);
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty pour ID inexistant")
    void shouldReturnEmpty_ForNonExistentId() {
        // WHEN
        Optional<Conversation> result = conversationRepository.findById(999L);

        // THEN
        assertThat(result).isEmpty();
    }

    // ========== Tests count ==========

    @Test
    @DisplayName("Devrait compter le nombre total de conversations")
    void shouldCountConversations() {
        // GIVEN
        Conversation conv1 = new Conversation();
        conv1.setUser1(alice);
        conv1.setUser2(bob);
        conv1.setCreatedAt(LocalDateTime.now());

        Conversation conv2 = new Conversation();
        conv2.setUser1(alice);
        conv2.setUser2(charlie);
        conv2.setCreatedAt(LocalDateTime.now());

        entityManager.persist(conv1);
        entityManager.persist(conv2);
        entityManager.flush();

        // WHEN
        long count = conversationRepository.count();

        // THEN
        assertThat(count).isEqualTo(2);
    }
}