package com.devops.projet_dialogue.controller;

import com.devops.projet_dialogue.model.Conversation;
import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.service.ConversationService;
import com.devops.projet_dialogue.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests du ConversationController
 */
@WebMvcTest(controllers = ConversationController.class)
@AutoConfigureMockMvc
@DisplayName("Tests du ConversationController")
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private UserService userService;

    private User alice;
    private User bob;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        // Créer les users avec des IDs
        alice = new User("alice", "password1", "ROLE_USER");
        alice.setId(1L);
        alice.setCreatedAt(LocalDateTime.now());

        bob = new User("bob", "password2", "ROLE_USER");
        bob.setId(2L);
        bob.setCreatedAt(LocalDateTime.now());

        conversation = new Conversation();
        conversation.setId(10L);
        conversation.setUser1(alice);
        conversation.setUser2(bob);
        conversation.setCreatedAt(LocalDateTime.now());
    }

    // ========== Tests GET /conversations ==========

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversations devrait afficher la liste des conversations")
    void shouldShowConversationsList() throws Exception {
        // GIVEN
        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));

        List<Conversation> conversations = Collections.singletonList(conversation);
        when(conversationService.findAllForUser(1L)).thenReturn(conversations);

        // WHEN & THEN
        mockMvc.perform(get("/conversations"))
                .andExpect(status().isOk())
                .andExpect(view().name("conversations"))
                .andExpect(model().attributeExists("conversations"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attribute("currentUser", alice))
                .andExpect(model().attribute("conversations", hasSize(1)));

        verify(userService, times(1)).findByUsername("alice");
        verify(conversationService, times(1)).findAllForUser(1L);
    }

    @Test
    @WithMockUser(username = "bob")
    @DisplayName("GET /conversations devrait retourner une liste vide si pas de conversations")
    void shouldShowEmptyList_WhenNoConversations() throws Exception {
        // GIVEN
        when(userService.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(conversationService.findAllForUser(2L)).thenReturn(List.of());

        // WHEN & THEN
        mockMvc.perform(get("/conversations"))
                .andExpect(status().isOk())
                .andExpect(view().name("conversations"))
                .andExpect(model().attribute("conversations", hasSize(0)));

        verify(conversationService, times(1)).findAllForUser(2L);
    }

    @Test
    @DisplayName("GET /conversations devrait nécessiter une authentification")
    void shouldRequireAuthentication_ForConversationsList() throws Exception {
        // WHEN & THEN - Sans @WithMockUser, Spring Security retourne 401
        mockMvc.perform(get("/conversations"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).findByUsername(anyString());
        verify(conversationService, never()).findAllForUser(anyLong());
    }

    @Test
    @WithMockUser(username = "unknown")
    @DisplayName("GET /conversations devrait afficher une page d'erreur si user non trouvé")
    void shouldShowErrorPage_WhenUserNotFound() throws Exception {
        // GIVEN
        when(userService.findByUsername("unknown")).thenReturn(Optional.empty());

        // WHEN & THEN - Affiche la page d'erreur via GlobalExceptionHandler
        mockMvc.perform(get("/conversations"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorTitle"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(userService, times(1)).findByUsername("unknown");
        verify(conversationService, never()).findAllForUser(anyLong());
    }

    // ========== Tests GET /conversations/with/{userId} ==========

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversations/with/{userId} devrait créer ou récupérer une conversation")
    void shouldOpenOrCreateConversation() throws Exception {
        // GIVEN
        Long bobId = 2L;

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversationService.getOrCreate(1L, bobId)).thenReturn(conversation);

        // WHEN & THEN
        mockMvc.perform(get("/conversations/with/" + bobId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conversation/" + conversation.getId()));

        verify(userService, times(1)).findByUsername("alice");
        verify(conversationService, times(1)).getOrCreate(1L, bobId);
    }

    @Test
    @WithMockUser(username = "bob")
    @DisplayName("GET /conversations/with/{userId} devrait gérer une conversation existante")
    void shouldOpenExistingConversation() throws Exception {
        // GIVEN
        Long aliceId = 1L;
        Long existingConvId = 5L;

        Conversation existingConv = new Conversation();
        existingConv.setId(existingConvId);
        existingConv.setUser1(bob);
        existingConv.setUser2(alice);

        when(userService.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(conversationService.getOrCreate(2L, aliceId)).thenReturn(existingConv);

        // WHEN & THEN
        mockMvc.perform(get("/conversations/with/" + aliceId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conversation/" + existingConvId));

        verify(conversationService, times(1)).getOrCreate(2L, aliceId);
    }

    @Test
    @DisplayName("GET /conversations/with/{userId} devrait nécessiter une authentification")
    void shouldRequireAuthentication_ForOpeningConversation() throws Exception {
        // WHEN & THEN - Sans @WithMockUser, Spring Security retourne 401
        mockMvc.perform(get("/conversations/with/2"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).findByUsername(anyString());
        verify(conversationService, never()).getOrCreate(anyLong(), anyLong());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversations/with/{userId} devrait afficher une page d'erreur si user cible non trouvé")
    void shouldShowErrorPage_WhenTargetUserNotFound() throws Exception {
        // GIVEN
        Long unknownUserId = 999L;

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversationService.getOrCreate(1L, unknownUserId))
                .thenThrow(new RuntimeException("User not found"));

        // WHEN & THEN - Affiche la page d'erreur via GlobalExceptionHandler
        mockMvc.perform(get("/conversations/with/" + unknownUserId))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(conversationService, times(1)).getOrCreate(1L, unknownUserId);
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversations/with/{userId} devrait gérer les IDs invalides")
    void shouldHandleInvalidUserId() throws Exception {
        // WHEN & THEN - ID non numérique provoque une erreur de conversion
        // Spring affiche une page d'erreur (statut 200) au lieu de 400
        mockMvc.perform(get("/conversations/with/invalid"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));

        verify(conversationService, never()).getOrCreate(anyLong(), anyLong());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversations/with/{userId} devrait permettre de créer une conversation avec soi-même")
    void shouldAllowConversationWithSelf() throws Exception {
        // GIVEN - Alice veut créer une conversation avec elle-même
        Long aliceId = 1L;

        Conversation selfConv = new Conversation();
        selfConv.setId(15L);
        selfConv.setUser1(alice);
        selfConv.setUser2(alice);

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversationService.getOrCreate(1L, aliceId)).thenReturn(selfConv);

        // WHEN & THEN
        mockMvc.perform(get("/conversations/with/" + aliceId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conversation/15"));

        verify(conversationService, times(1)).getOrCreate(1L, aliceId);
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversations devrait charger plusieurs conversations")
    void shouldLoadMultipleConversations() throws Exception {
        // GIVEN - Alice a 3 conversations
        User charlie = new User("charlie", "pass", "ROLE_USER");
        charlie.setId(3L);

        Conversation conv1 = new Conversation();
        conv1.setId(1L);
        conv1.setUser1(alice);
        conv1.setUser2(bob);

        Conversation conv2 = new Conversation();
        conv2.setId(2L);
        conv2.setUser1(alice);
        conv2.setUser2(charlie);

        Conversation conv3 = new Conversation();
        conv3.setId(3L);
        conv3.setUser1(charlie);
        conv3.setUser2(alice);

        List<Conversation> conversations = Arrays.asList(conv1, conv2, conv3);

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversationService.findAllForUser(1L)).thenReturn(conversations);

        // WHEN & THEN
        mockMvc.perform(get("/conversations"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("conversations", hasSize(3)));

        verify(conversationService, times(1)).findAllForUser(1L);
    }
}