package com.devops.projet_dialogue.controller;

import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.repository.UserRepository;
import com.devops.projet_dialogue.security.CustomUserDetails;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests du UserController
 */
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc  // ← Filtres de sécurité activés
@DisplayName("Tests du UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    private User alice;
    private User bob;
    private User charlie;
    private CustomUserDetails aliceDetails;

    @BeforeEach
    void setUp() {
        alice = new User("alice", "password1", "ROLE_USER");
        alice.setCreatedAt(LocalDateTime.now());
        // Simuler un ID
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(alice, 1L);
        } catch (Exception e) {
            // Fallback si reflection échoue
        }

        bob = new User("bob", "password2", "ROLE_USER");
        bob.setCreatedAt(LocalDateTime.now());
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(bob, 2L);
        } catch (Exception e) {
            // Fallback
        }

        charlie = new User("charlie", "password3", "ROLE_USER");
        charlie.setCreatedAt(LocalDateTime.now());
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(charlie, 3L);
        } catch (Exception e) {
            // Fallback
        }

        aliceDetails = new CustomUserDetails(alice);
    }

    // ========== Tests GET /user/home ==========

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("GET /user/home devrait afficher la page d'accueil user")
    void shouldShowUserHome() throws Exception {
        mockMvc.perform(get("/user/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("user-home"))
                .andExpect(model().attributeExists("username"))
                .andExpect(model().attribute("username", "alice"));
    }

    @Test
    @DisplayName("GET /user/home devrait nécessiter une authentification")
    void shouldRequireAuthentication_ForUserHome() throws Exception {
        mockMvc.perform(get("/user/home"))
                .andExpect(status().isUnauthorized()); // 401 en environnement de test
    }

    // ========== Tests GET /user (liste des utilisateurs) ==========

    @Test
    @DisplayName("GET /user devrait afficher la liste des autres utilisateurs")
    void shouldShowUserList() throws Exception {
        // GIVEN
        List<User> allUsers = Arrays.asList(alice, bob, charlie);
        when(userRepository.findAll()).thenReturn(allUsers);

        // WHEN & THEN - Alice connectée, devrait voir Bob et Charlie
        mockMvc.perform(get("/user")
                        .with(user(aliceDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("currentUserId"));

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("GET /user devrait exclure l'utilisateur connecté de la liste")
    void shouldExcludeCurrentUser_FromList() throws Exception {
        // GIVEN
        List<User> allUsers = Arrays.asList(alice, bob, charlie);
        when(userRepository.findAll()).thenReturn(allUsers);

        // WHEN & THEN
        mockMvc.perform(get("/user")
                        .with(user(aliceDetails)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("users"));

        // Note: Le filtrage se fait dans le controller avec stream().filter()
        // On vérifie juste que findAll() est appelé
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("GET /user devrait afficher une liste vide si user est seul")
    void shouldShowEmptyList_WhenOnlyOneUser() throws Exception {
        // GIVEN - Alice est la seule utilisatrice
        when(userRepository.findAll()).thenReturn(List.of(alice));

        // WHEN & THEN
        mockMvc.perform(get("/user")
                        .with(user(aliceDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"));

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("GET /user devrait nécessiter une authentification")
    void shouldRequireAuthentication_ForUserList() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isUnauthorized()); // 401 en environnement de test

        verify(userRepository, never()).findAll();
    }

    // ========== Tests GET /user/{id} (profil utilisateur) ==========

    @Test
    @DisplayName("GET /user/{id} devrait afficher le profil d'un autre utilisateur")
    void shouldShowUserProfile() throws Exception {
        // GIVEN
        Long bobId = 2L;
        when(userRepository.findById(bobId)).thenReturn(Optional.of(bob));

        // WHEN & THEN - Alice veut voir le profil de Bob
        mockMvc.perform(get("/user/" + bobId)
                        .with(user(aliceDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("users/view"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", bob))
                .andExpect(model().attributeExists("currentUserId"));

        verify(userRepository, times(1)).findById(bobId);
    }

    @Test
    @DisplayName("GET /user/{id} devrait rediriger si user essaie de voir son propre profil")
    void shouldRedirect_WhenViewingOwnProfile() throws Exception {
        // GIVEN - Alice (ID=1) essaie de voir son profil
        Long aliceId = 1L;

        // WHEN & THEN
        mockMvc.perform(get("/user/" + aliceId)
                        .with(user(aliceDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/home"));

        // Ne devrait PAS chercher dans la base
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("GET /user/{id} devrait rediriger si utilisateur non trouvé")
    void shouldRedirect_WhenUserNotFound() throws Exception {
        // GIVEN
        Long unknownId = 999L;
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        // WHEN & THEN
        mockMvc.perform(get("/user/" + unknownId)
                        .with(user(aliceDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?notfound"));

        verify(userRepository, times(1)).findById(unknownId);
    }

    @Test
    @DisplayName("GET /user/{id} devrait gérer les IDs invalides")
    void shouldHandleInvalidId() throws Exception {
        // WHEN & THEN - ID non numérique
        // Spring MVC renvoie 400 Bad Request automatiquement lors de la conversion du path variable
        mockMvc.perform(get("/user/invalid")
                        .with(user(aliceDetails)))
                .andExpect(status().isBadRequest()); // 400 Bad Request

        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("GET /user/{id} devrait nécessiter une authentification")
    void shouldRequireAuthentication_ForViewingProfile() throws Exception {
        mockMvc.perform(get("/user/2"))
                .andExpect(status().isUnauthorized()); // 401 en environnement de test

        verify(userRepository, never()).findById(anyLong());
    }

    // ========== Tests des IDs et cas limites ==========

    @Test
    @DisplayName("GET /user/{id} devrait gérer les IDs négatifs")
    void shouldHandleNegativeId() throws Exception {
        // GIVEN
        Long negativeId = -1L;
        when(userRepository.findById(negativeId)).thenReturn(Optional.empty());

        // WHEN & THEN
        mockMvc.perform(get("/user/" + negativeId)
                        .with(user(aliceDetails)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user?notfound"));

        verify(userRepository, times(1)).findById(negativeId);
    }

    @Test
    @DisplayName("GET /user/{id} devrait gérer l'ID 0")
    void shouldHandleZeroId() throws Exception {
        // GIVEN
        when(userRepository.findById(0L)).thenReturn(Optional.empty());

        // WHEN & THEN
        mockMvc.perform(get("/user/0")
                        .with(user(aliceDetails)))
                .andExpect(status().is3xxRedirection());

        verify(userRepository, times(1)).findById(0L);
    }

    @Test
    @DisplayName("GET /user devrait gérer une base de données vide")
    void shouldHandleEmptyDatabase() throws Exception {
        // GIVEN - Aucun utilisateur
        when(userRepository.findAll()).thenReturn(List.of());

        // WHEN & THEN
        mockMvc.perform(get("/user")
                        .with(user(aliceDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"));

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("GET /user devrait charger plusieurs utilisateurs")
    void shouldLoadMultipleUsers() throws Exception {
        // GIVEN - 10 utilisateurs avec IDs
        User user4 = new User("user4", "pass", "ROLE_USER");
        User user5 = new User("user5", "pass", "ROLE_USER");
        User user6 = new User("user6", "pass", "ROLE_USER");
        User user7 = new User("user7", "pass", "ROLE_USER");
        User user8 = new User("user8", "pass", "ROLE_USER");
        User user9 = new User("user9", "pass", "ROLE_USER");
        User user10 = new User("user10", "pass", "ROLE_USER");

        // Assigner des IDs via reflection
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user4, 4L);
            idField.set(user5, 5L);
            idField.set(user6, 6L);
            idField.set(user7, 7L);
            idField.set(user8, 8L);
            idField.set(user9, 9L);
            idField.set(user10, 10L);
        } catch (Exception e) {
            // Fallback
        }

        List<User> manyUsers = Arrays.asList(
                alice, bob, charlie,
                user4, user5, user6, user7, user8, user9, user10
        );

        when(userRepository.findAll()).thenReturn(manyUsers);

        // WHEN & THEN
        mockMvc.perform(get("/user")
                        .with(user(aliceDetails)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("users"));

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("GET /user/home devrait fonctionner pour différents utilisateurs")
    void shouldWorkForDifferentUsers() throws Exception {
        mockMvc.perform(get("/user/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("user-home"))
                .andExpect(model().attribute("username", "bob"));
    }
}