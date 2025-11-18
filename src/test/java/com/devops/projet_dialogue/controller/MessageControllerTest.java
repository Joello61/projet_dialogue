package com.devops.projet_dialogue.controller;

import com.devops.projet_dialogue.model.Conversation;
import com.devops.projet_dialogue.model.Message;
import com.devops.projet_dialogue.model.Photo;
import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.service.ConversationService;
import com.devops.projet_dialogue.service.MessageService;
import com.devops.projet_dialogue.service.PhotoService;
import com.devops.projet_dialogue.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests du MessageController
 */
@WebMvcTest(controllers = MessageController.class)
@AutoConfigureMockMvc  // ← Filtres de sécurité activés
@DisplayName("Tests du MessageController")
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PhotoService photoService;

    private User alice;
    private User bob;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        alice = new User("alice", "password1", "ROLE_USER");
        alice.setCreatedAt(LocalDateTime.now());

        bob = new User("bob", "password2", "ROLE_USER");
        bob.setCreatedAt(LocalDateTime.now());

        conversation = new Conversation();
        conversation.setUser1(alice);
        conversation.setUser2(bob);
        conversation.setCreatedAt(LocalDateTime.now());
    }

    // ========== Tests GET /conversation/{id} ==========

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversation/{id} devrait afficher la conversation")
    void shouldShowConversation() throws Exception {
        // GIVEN
        Long conversationId = 1L;

        Message msg1 = new Message();
        msg1.setText("Hello");
        msg1.setSender(alice);

        Message msg2 = new Message();
        msg2.setText("Hi!");
        msg2.setSender(bob);

        List<Message> messages = Arrays.asList(msg1, msg2);

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversationService.findById(conversationId)).thenReturn(conversation);
        when(messageService.listMessages(conversationId)).thenReturn(messages);

        // WHEN & THEN
        mockMvc.perform(get("/conversation/" + conversationId))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attributeExists("conversation"))
                .andExpect(model().attributeExists("messages"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("otherUser"))
                .andExpect(model().attribute("currentUser", alice))
                .andExpect(model().attribute("otherUser", bob));

        verify(conversationService, times(1)).findById(conversationId);
        verify(messageService, times(1)).listMessages(conversationId);
    }

    @Test
    @WithMockUser(username = "bob")
    @DisplayName("GET /conversation/{id} devrait identifier l'autre utilisateur correctement")
    void shouldIdentifyOtherUser_Correctly() throws Exception {
        // GIVEN
        Long conversationId = 1L;

        when(userService.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(conversationService.findById(conversationId)).thenReturn(conversation);
        when(messageService.listMessages(conversationId)).thenReturn(List.of());

        // WHEN & THEN - Bob est connecté, donc otherUser = Alice
        mockMvc.perform(get("/conversation/" + conversationId))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentUser", bob))
                .andExpect(model().attribute("otherUser", alice));
    }

    @Test
    @WithMockUser(username = "charlie")
    @DisplayName("GET /conversation/{id} devrait rediriger si user non participant")
    void shouldRedirect_WhenUserNotParticipant() throws Exception {
        // GIVEN - Charlie n'est pas dans cette conversation
        Long conversationId = 1L;
        User charlie = new User("charlie", "pass", "ROLE_USER");

        when(userService.findByUsername("charlie")).thenReturn(Optional.of(charlie));
        when(conversationService.findById(conversationId)).thenReturn(conversation);

        // WHEN & THEN
        mockMvc.perform(get("/conversation/" + conversationId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conversations"));

        verify(messageService, never()).listMessages(anyLong());
    }

    @Test
    @DisplayName("GET /conversation/{id} devrait nécessiter une authentification")
    void shouldRequireAuthentication_ForViewingConversation() throws Exception {
        mockMvc.perform(get("/conversation/1"))
                .andExpect(status().isUnauthorized()); // 401 en environnement de test

        verify(conversationService, never()).findById(anyLong());
    }

    // ========== Tests POST /conversation/{id}/send ==========

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("POST /conversation/{id}/send devrait envoyer un message texte")
    void shouldSendTextMessage() throws Exception {
        // GIVEN
        Long conversationId = 1L;

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));

        Message savedMessage = new Message();
        savedMessage.setText("Hello Bob!");
        when(messageService.sendMessage(eq(conversationId), eq(alice), anyString(), isNull()))
                .thenReturn(savedMessage);

        // WHEN & THEN
        mockMvc.perform(multipart("/conversation/" + conversationId + "/send")
                        .param("text", "Hello Bob!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conversation/" + conversationId));

        verify(messageService, times(1)).sendMessage(conversationId, alice, "Hello Bob!", null);
        verify(photoService, never()).savePhoto(any(), any());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("POST /conversation/{id}/send devrait envoyer une photo seule")
    void shouldSendPhotoOnly() throws Exception {
        // GIVEN
        Long conversationId = 1L;

        MockMultipartFile photo = new MockMultipartFile(
                "image",
                "photo.jpg",
                "image/jpeg",
                "fake-image-content".getBytes()
        );

        Photo savedPhoto = new Photo("uuid.jpg", "photo.jpg", "/uploads/uuid.jpg", alice);

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(photoService.savePhoto(any(), eq(alice))).thenReturn(savedPhoto);

        // WHEN & THEN
        mockMvc.perform(multipart("/conversation/" + conversationId + "/send")
                        .file(photo)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conversation/" + conversationId));

        verify(photoService, times(1)).savePhoto(any(), eq(alice));
        verify(messageService, times(1)).sendMessage(eq(conversationId), eq(alice), isNull(), eq(savedPhoto));
    }

    @Test
    @WithMockUser(username = "bob")
    @DisplayName("POST /conversation/{id}/send devrait envoyer texte + photo")
    void shouldSendTextAndPhoto() throws Exception {
        // GIVEN
        Long conversationId = 1L;

        MockMultipartFile photo = new MockMultipartFile(
                "image",
                "screenshot.png",
                "image/png",
                "fake-png-content".getBytes()
        );

        Photo savedPhoto = new Photo("uuid.png", "screenshot.png", "/uploads/uuid.png", bob);

        when(userService.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(photoService.savePhoto(any(), eq(bob))).thenReturn(savedPhoto);

        // WHEN & THEN
        mockMvc.perform(multipart("/conversation/" + conversationId + "/send")
                        .file(photo)
                        .param("text", "Check this out!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conversation/" + conversationId));

        verify(photoService, times(1)).savePhoto(any(), eq(bob));
        verify(messageService, times(1)).sendMessage(eq(conversationId), eq(bob), eq("Check this out!"), eq(savedPhoto));
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("POST /conversation/{id}/send devrait gérer une photo vide")
    void shouldHandleEmptyPhoto() throws Exception {
        // GIVEN
        Long conversationId = 1L;

        MockMultipartFile emptyPhoto = new MockMultipartFile(
                "image",
                "",
                "image/jpeg",
                new byte[0]
        );

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));

        // WHEN & THEN
        mockMvc.perform(multipart("/conversation/" + conversationId + "/send")
                        .file(emptyPhoto)
                        .param("text", "Text only")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Ne devrait PAS essayer de sauvegarder la photo vide
        verify(photoService, never()).savePhoto(any(), any());
        verify(messageService, times(1)).sendMessage(eq(conversationId), eq(alice), eq("Text only"), isNull());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("POST /conversation/{id}/send devrait continuer si sauvegarde photo échoue")
    void shouldContinue_WhenPhotoSaveFails() throws Exception {
        // GIVEN
        Long conversationId = 1L;

        MockMultipartFile photo = new MockMultipartFile(
                "image",
                "corrupted.jpg",
                "image/jpeg",
                "corrupted-data".getBytes()
        );

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(photoService.savePhoto(any(), any())).thenThrow(new RuntimeException("IO Error"));

        // WHEN & THEN - Devrait quand même rediriger
        mockMvc.perform(multipart("/conversation/" + conversationId + "/send")
                        .file(photo)
                        .param("text", "Text message")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conversation/" + conversationId));

        // Message envoyé sans photo
        verify(messageService, times(1)).sendMessage(eq(conversationId), eq(alice), eq("Text message"), isNull());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("POST /conversation/{id}/send devrait nécessiter CSRF token")
    void shouldRequireCsrfToken_ForSendingMessage() throws Exception {
        // WHEN & THEN - POST sans CSRF
        mockMvc.perform(multipart("/conversation/1/send")
                        .param("text", "Test"))
                .andExpect(status().isForbidden()); // 403

        verify(messageService, never()).sendMessage(anyLong(), any(), anyString(), any());
    }

    @Test
    @DisplayName("POST /conversation/{id}/send devrait nécessiter une authentification")
    void shouldRequireAuthentication_ForSendingMessage() throws Exception {
        mockMvc.perform(multipart("/conversation/1/send")
                        .param("text", "Test")
                        .with(csrf()))
                .andExpect(status().isUnauthorized()); // 401 en environnement de test

        verify(messageService, never()).sendMessage(anyLong(), any(), anyString(), any());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("POST /conversation/{id}/send devrait gérer les messages vides")
    void shouldHandleEmptyMessage() throws Exception {
        // GIVEN
        Long conversationId = 1L;

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));

        // WHEN & THEN - Texte vide, pas de photo
        mockMvc.perform(multipart("/conversation/" + conversationId + "/send")
                        .param("text", "")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(messageService, times(1)).sendMessage(eq(conversationId), eq(alice), eq(""), isNull());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("POST /conversation/{id}/send devrait gérer les textes longs")
    void shouldHandleLongText() throws Exception {
        // GIVEN
        Long conversationId = 1L;
        String longText = "A".repeat(5000); // 5000 caractères

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));

        // WHEN & THEN
        mockMvc.perform(multipart("/conversation/" + conversationId + "/send")
                        .param("text", longText)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(messageService, times(1)).sendMessage(eq(conversationId), eq(alice), eq(longText), isNull());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversation/{id} devrait gérer une conversation vide")
    void shouldHandleEmptyConversation() throws Exception {
        // GIVEN
        Long conversationId = 1L;

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversationService.findById(conversationId)).thenReturn(conversation);
        when(messageService.listMessages(conversationId)).thenReturn(List.of());

        // WHEN & THEN
        mockMvc.perform(get("/conversation/" + conversationId))
                .andExpect(status().isOk())
                .andExpect(model().attribute("messages", List.of()));
    }
}