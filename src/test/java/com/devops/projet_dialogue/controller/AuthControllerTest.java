package com.devops.projet_dialogue.controller;

import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests du AuthController
 * @WebMvcTest charge uniquement le controller et ses dépendances
 * @AutoConfigureMockMvc avec addFilters=false désactive Spring Security pour les tests
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Tests du AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    // ========== Tests GET /login ==========

    @Test
    @DisplayName("GET /login devrait afficher la page de login")
    void shouldShowLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    // ========== Tests GET /register ==========

    @Test
    @DisplayName("GET /register devrait afficher le formulaire d'inscription")
    void shouldShowRegisterForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerRequest"));
    }

    // ========== Tests POST /register - Cas valides ==========

    @Test
    @DisplayName("POST /register devrait créer un nouveau user avec succès")
    void shouldRegisterNewUser_Successfully() throws Exception {
        // GIVEN
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN & THEN
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "alice")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        // Vérifier que l'utilisateur a été sauvegardé
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("POST /register devrait encoder le mot de passe")
    void shouldEncodePassword_WhenRegistering() throws Exception {
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");

        // WHEN
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "bob")
                        .param("password", "secretPassword")
                        .param("confirmPassword", "secretPassword"))
                .andExpect(status().is3xxRedirection());

        // THEN
        verify(passwordEncoder, times(1)).encode("secretPassword");
    }

    @Test
    @DisplayName("POST /register devrait attribuer le rôle ROLE_USER par défaut")
    void shouldAssignRoleUser_ByDefault() throws Exception {
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        // WHEN
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("password", "pass123")
                        .param("confirmPassword", "pass123"))
                .andExpect(status().is3xxRedirection());

        // THEN - Vérifier que le rôle est ROLE_USER
        verify(userRepository).save(argThat(user ->
                user.getRole().equals("ROLE_USER")
        ));
    }

    // ========== Tests POST /register - Erreurs de validation ==========

    @Test
    @DisplayName("POST /register devrait rejeter si mots de passe différents")
    void shouldRejectRegistration_WhenPasswordsDontMatch() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "alice")
                        .param("password", "password123")
                        .param("confirmPassword", "password456"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "Les mots de passe ne correspondent pas."));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("POST /register devrait rejeter si username déjà pris")
    void shouldRejectRegistration_WhenUsernameAlreadyExists() throws Exception {
        // GIVEN - Username existe déjà
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        // WHEN & THEN
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "alice")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "Ce nom d'utilisateur est déjà pris."));

        verify(userRepository, never()).save(any(User.class));
    }

    // ========== Tests cas limites ==========

    @Test
    @DisplayName("POST /register devrait gérer un username vide")
    void shouldHandleEmptyUsername() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("POST /register devrait gérer un mot de passe vide")
    void shouldHandleEmptyPassword() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "alice")
                        .param("password", "")
                        .param("confirmPassword", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("POST /register devrait vérifier l'existence du username avant d'encoder le mot de passe")
    void shouldCheckUsernameExists_BeforeEncodingPassword() throws Exception {
        // GIVEN
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        // WHEN
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "existing")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk());

        // THEN - Ne devrait PAS encoder le mot de passe si username existe
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("POST /register devrait gérer les espaces dans le username")
    void shouldHandleWhitespaceInUsername() throws Exception {
        // GIVEN
        when(userRepository.existsByUsername("user name")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        // WHEN
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "user name")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection());

        // THEN
        verify(userRepository).save(argThat(user ->
                user.getUsername().equals("user name")
        ));
    }
}