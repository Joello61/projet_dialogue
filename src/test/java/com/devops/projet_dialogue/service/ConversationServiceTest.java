package com.devops.projet_dialogue.service;

import com.devops.projet_dialogue.model.Conversation;
import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.repository.ConversationRepository;
import com.devops.projet_dialogue.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ConversationService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du ConversationService")
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConversationService conversationService;

    private User alice;
    private User bob;
    private Conversation existingConversation;

    @BeforeEach
    void setUp() {
        // Préparation des utilisateurs de test
        alice = new User("alice", "password1", "ROLE_USER");
        alice.setCreatedAt(LocalDateTime.now());

        bob = new User("bob", "password2", "ROLE_USER");
        bob.setCreatedAt(LocalDateTime.now());

        // Conversation existante entre Alice et Bob
        existingConversation = new Conversation();
        existingConversation.setUser1(alice);
        existingConversation.setUser2(bob);
        existingConversation.setCreatedAt(LocalDateTime.now());
    }

    // ========== Tests getOrCreate ==========

    @Test
    @DisplayName("Devrait retourner une conversation existante")
    void shouldReturnExistingConversation_WhenConversationExists() {
        // GIVEN - La conversation existe déjà
        Long aliceId = 1L;
        Long bobId = 2L;

        when(conversationRepository.findByUsers(aliceId, bobId))
                .thenReturn(Optional.of(existingConversation));

        // WHEN
        Conversation result = conversationService.getOrCreate(aliceId, bobId);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getUser1()).isEqualTo(alice);
        assertThat(result.getUser2()).isEqualTo(bob);

        // Vérifier qu'on n'a PAS créé de nouvelle conversation
        verify(conversationRepository, times(1)).findByUsers(aliceId, bobId);
        verify(conversationRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Devrait créer une nouvelle conversation si elle n'existe pas")
    void shouldCreateNewConversation_WhenConversationDoesNotExist() {
        // GIVEN - La conversation n'existe pas
        Long aliceId = 1L;
        Long bobId = 2L;

        when(conversationRepository.findByUsers(aliceId, bobId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(aliceId))
                .thenReturn(Optional.of(alice));
        when(userRepository.findById(bobId))
                .thenReturn(Optional.of(bob));
        when(conversationRepository.save(any(Conversation.class)))
                .thenReturn(existingConversation);

        // WHEN
        Conversation result = conversationService.getOrCreate(aliceId, bobId);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getUser1()).isEqualTo(alice);
        assertThat(result.getUser2()).isEqualTo(bob);

        // Vérifier qu'on a bien créé la conversation
        verify(conversationRepository, times(1)).findByUsers(aliceId, bobId);
        verify(userRepository, times(1)).findById(aliceId);
        verify(userRepository, times(1)).findById(bobId);
        verify(conversationRepository, times(1)).save(any(Conversation.class));
    }

    @Test
    @DisplayName("Devrait lever une exception si userA n'existe pas")
    void shouldThrowException_WhenUserANotFound() {
        // GIVEN
        Long aliceId = 999L;
        Long bobId = 2L;

        when(conversationRepository.findByUsers(aliceId, bobId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(aliceId))
                .thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> conversationService.getOrCreate(aliceId, bobId))
                .isInstanceOf(NoSuchElementException.class);

        verify(conversationRepository, times(1)).findByUsers(aliceId, bobId);
        verify(userRepository, times(1)).findById(aliceId);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait lever une exception si userB n'existe pas")
    void shouldThrowException_WhenUserBNotFound() {
        // GIVEN
        Long aliceId = 1L;
        Long bobId = 999L;

        when(conversationRepository.findByUsers(aliceId, bobId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(aliceId))
                .thenReturn(Optional.of(alice));
        when(userRepository.findById(bobId))
                .thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> conversationService.getOrCreate(aliceId, bobId))
                .isInstanceOf(NoSuchElementException.class);

        verify(userRepository, times(1)).findById(aliceId);
        verify(userRepository, times(1)).findById(bobId);
        verify(conversationRepository, never()).save(any());
    }

    // ========== Tests findAllForUser ==========

    @Test
    @DisplayName("Devrait retourner toutes les conversations d'un utilisateur")
    void shouldFindAllConversationsForUser() {
        // GIVEN
        Long userId = 1L;

        Conversation conv1 = new Conversation();
        conv1.setUser1(alice);
        conv1.setUser2(bob);

        User charlie = new User("charlie", "pass", "ROLE_USER");
        Conversation conv2 = new Conversation();
        conv2.setUser1(alice);
        conv2.setUser2(charlie);

        List<Conversation> conversations = Arrays.asList(conv1, conv2);

        when(conversationRepository.findAllForUser(userId))
                .thenReturn(conversations);

        // WHEN
        List<Conversation> result = conversationService.findAllForUser(userId);

        // THEN
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(conv1, conv2);
        verify(conversationRepository, times(1)).findAllForUser(userId);
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si l'utilisateur n'a pas de conversations")
    void shouldReturnEmptyList_WhenUserHasNoConversations() {
        // GIVEN
        Long userId = 1L;
        when(conversationRepository.findAllForUser(userId))
                .thenReturn(List.of());

        // WHEN
        List<Conversation> result = conversationService.findAllForUser(userId);

        // THEN
        assertThat(result).isEmpty();
        verify(conversationRepository, times(1)).findAllForUser(userId);
    }

    // ========== Tests findById ==========

    @Test
    @DisplayName("Devrait trouver une conversation par ID")
    void shouldFindConversationById_WhenConversationExists() {
        // GIVEN
        Long conversationId = 1L;
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(existingConversation));

        // WHEN
        Conversation result = conversationService.findById(conversationId);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(existingConversation);
        verify(conversationRepository, times(1)).findById(conversationId);
    }

    @Test
    @DisplayName("Devrait lever une exception si conversation non trouvée")
    void shouldThrowException_WhenConversationNotFound() {
        // GIVEN
        Long conversationId = 999L;
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> conversationService.findById(conversationId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Conversation non trouvée");

        verify(conversationRepository, times(1)).findById(conversationId);
    }

    @Test
    @DisplayName("Devrait gérer les IDs nuls proprement")
    void shouldHandleNullIdGracefully() {
        // GIVEN
        when(conversationRepository.findById(null))
                .thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> conversationService.findById(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Conversation non trouvée");
    }
}