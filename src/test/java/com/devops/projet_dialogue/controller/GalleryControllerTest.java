package com.devops.projet_dialogue.controller;

import com.devops.projet_dialogue.model.Conversation;
import com.devops.projet_dialogue.model.Photo;
import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.service.ConversationService;
import com.devops.projet_dialogue.service.MessageService;
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
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GalleryController.class)
@AutoConfigureMockMvc
@DisplayName("Tests du GalleryController")
class GalleryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private UserService userService;

    private User alice;
    private User bob;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        // Créer les users AVEC des IDs
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

    // ========== Tests GET /conversation/{id}/gallery ==========

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversation/{id}/gallery devrait afficher la galerie")
    void shouldShowGallery() throws Exception {
        // GIVEN
        Long conversationId = 1L;

        Photo photo1 = new Photo("uuid1.jpg", "photo1.jpg", "/uploads/uuid1.jpg", alice);
        Photo photo2 = new Photo("uuid2.jpg", "photo2.jpg", "/uploads/uuid2.jpg", bob);
        List<Photo> photos = Arrays.asList(photo1, photo2);

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversationService.findById(conversationId)).thenReturn(conversation);
        when(messageService.listPhotos(conversationId)).thenReturn(photos);

        // WHEN & THEN
        mockMvc.perform(get("/conversation/" + conversationId + "/gallery"))
                .andExpect(status().isOk())
                .andExpect(view().name("gallery"))
                .andExpect(model().attributeExists("conversation"))
                .andExpect(model().attributeExists("photos"))
                .andExpect(model().attributeExists("otherUser"))
                .andExpect(model().attribute("photos", photos))
                .andExpect(model().attribute("otherUser", bob));

        verify(conversationService, times(1)).findById(conversationId);
        verify(messageService, times(1)).listPhotos(conversationId);
    }

    @Test
    @WithMockUser(username = "bob")
    @DisplayName("GET /conversation/{id}/gallery devrait identifier l'autre user correctement")
    void shouldIdentifyOtherUser_Correctly() throws Exception {
        // GIVEN
        Long conversationId = 1L;

        when(userService.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(conversationService.findById(conversationId)).thenReturn(conversation);
        when(messageService.listPhotos(conversationId)).thenReturn(List.of());

        // WHEN & THEN - Bob est connecté, donc otherUser = Alice
        mockMvc.perform(get("/conversation/" + conversationId + "/gallery"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("otherUser", alice));
    }

    @Test
    @WithMockUser(username = "charlie")
    @DisplayName("GET /conversation/{id}/gallery devrait rediriger si user non participant")
    void shouldRedirect_WhenUserNotParticipant() throws Exception {
        // GIVEN - Charlie n'est pas dans cette conversation
        Long conversationId = 1L;
        User charlie = new User("charlie", "pass", "ROLE_USER");
        charlie.setId(3L); // ID différent de alice et bob

        when(userService.findByUsername("charlie")).thenReturn(Optional.of(charlie));
        when(conversationService.findById(conversationId)).thenReturn(conversation);

        // WHEN & THEN
        mockMvc.perform(get("/conversation/" + conversationId + "/gallery"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conversations"));

        // Ne devrait PAS charger les photos
        verify(messageService, never()).listPhotos(anyLong());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversation/{id}/gallery devrait afficher une galerie vide")
    void shouldShowEmptyGallery() throws Exception {
        // GIVEN - Conversation sans photos
        Long conversationId = 1L;

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversationService.findById(conversationId)).thenReturn(conversation);
        when(messageService.listPhotos(conversationId)).thenReturn(List.of());

        // WHEN & THEN
        mockMvc.perform(get("/conversation/" + conversationId + "/gallery"))
                .andExpect(status().isOk())
                .andExpect(view().name("gallery"))
                .andExpect(model().attribute("photos", hasSize(0)));

        verify(messageService, times(1)).listPhotos(conversationId);
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversation/{id}/gallery devrait charger plusieurs photos")
    void shouldLoadMultiplePhotos() throws Exception {
        // GIVEN - 5 photos
        Long conversationId = 1L;

        List<Photo> photos = Arrays.asList(
                new Photo("uuid1.jpg", "photo1.jpg", "/uploads/uuid1.jpg", alice),
                new Photo("uuid2.jpg", "photo2.jpg", "/uploads/uuid2.jpg", bob),
                new Photo("uuid3.jpg", "photo3.jpg", "/uploads/uuid3.jpg", alice),
                new Photo("uuid4.jpg", "photo4.jpg", "/uploads/uuid4.jpg", bob),
                new Photo("uuid5.jpg", "photo5.jpg", "/uploads/uuid5.jpg", alice)
        );

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversationService.findById(conversationId)).thenReturn(conversation);
        when(messageService.listPhotos(conversationId)).thenReturn(photos);

        // WHEN & THEN
        mockMvc.perform(get("/conversation/" + conversationId + "/gallery"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("photos", hasSize(5)));

        verify(messageService, times(1)).listPhotos(conversationId);
    }

    @Test
    @DisplayName("GET /conversation/{id}/gallery devrait nécessiter une authentification")
    void shouldRequireAuthentication() throws Exception {
        // WHEN & THEN - Sans @WithMockUser, retourne 401
        mockMvc.perform(get("/conversation/1/gallery"))
                .andExpect(status().isUnauthorized());

        verify(conversationService, never()).findById(anyLong());
        verify(messageService, never()).listPhotos(anyLong());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversation/{id}/gallery devrait gérer les IDs invalides")
    void shouldHandleInvalidId() throws Exception {
        // WHEN & THEN - ID non numérique affiche page d'erreur
        mockMvc.perform(get("/conversation/invalid/gallery"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));

        verify(conversationService, never()).findById(anyLong());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversation/{id}/gallery devrait afficher page d'erreur si conversation inexistante")
    void shouldShowErrorPage_WhenConversationNotFound() throws Exception {
        // GIVEN
        Long conversationId = 999L;

        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(conversationService.findById(conversationId))
                .thenThrow(new RuntimeException("Conversation non trouvée"));

        // WHEN & THEN - Affiche page d'erreur via GlobalExceptionHandler
        mockMvc.perform(get("/conversation/" + conversationId + "/gallery"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(conversationService, times(1)).findById(conversationId);
        verify(messageService, never()).listPhotos(anyLong());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /conversation/{id}/gallery devrait vérifier sécurité avant de charger photos")
    void shouldCheckSecurity_BeforeLoadingPhotos() throws Exception {
        // GIVEN - Charlie essaie d'accéder à la galerie d'Alice et Bob
        Long conversationId = 1L;
        User charlie = new User("charlie", "pass", "ROLE_USER");
        charlie.setId(3L);

        when(userService.findByUsername("alice")).thenReturn(Optional.of(charlie));
        when(conversationService.findById(conversationId)).thenReturn(conversation);

        // WHEN
        mockMvc.perform(get("/conversation/" + conversationId + "/gallery"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conversations"));

        // THEN - Les photos ne devraient PAS être chargées
        verify(messageService, never()).listPhotos(conversationId);
    }
}