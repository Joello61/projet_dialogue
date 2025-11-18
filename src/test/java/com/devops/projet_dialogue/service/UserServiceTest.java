package com.devops.projet_dialogue.service;

import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour UserService
 * On utilise Mockito pour simuler le repository
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Préparation d'un utilisateur de test avant chaque test
        testUser = new User();
        testUser.setUsername("alice");
        testUser.setPassword("$2a$10$hashedPassword");
        testUser.setRole("ROLE_USER");
        testUser.setCreatedAt(LocalDateTime.now());
    }

    // ========== Tests findByUsername ==========

    @Test
    @DisplayName("Devrait trouver un utilisateur par username existant")
    void shouldFindUserByUsername_WhenUserExists() {
        // GIVEN - On simule que le repository retourne un user
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(testUser));

        // WHEN - On appelle le service
        Optional<User> result = userService.findByUsername("alice");

        // THEN - On vérifie le résultat
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");

        // Vérifier que le repository a bien été appelé 1 fois
        verify(userRepository, times(1)).findByUsername("alice");
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty si username inexistant")
    void shouldReturnEmpty_WhenUsernameNotFound() {
        // GIVEN
        when(userRepository.findByUsername("inconnu"))
                .thenReturn(Optional.empty());

        // WHEN
        Optional<User> result = userService.findByUsername("inconnu");

        // THEN
        assertThat(result).isEmpty();
        verify(userRepository, times(1)).findByUsername("inconnu");
    }

    // ========== Tests findById ==========

    @Test
    @DisplayName("Devrait trouver un utilisateur par ID existant")
    void shouldFindUserById_WhenIdExists() {
        // GIVEN
        Long userId = 1L;
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));

        // WHEN
        Optional<User> result = userService.findById(userId);

        // THEN
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty si ID inexistant")
    void shouldReturnEmpty_WhenIdNotFound() {
        // GIVEN
        Long unknownId = 999L;
        when(userRepository.findById(unknownId))
                .thenReturn(Optional.empty());

        // WHEN
        Optional<User> result = userService.findById(unknownId);

        // THEN
        assertThat(result).isEmpty();
        verify(userRepository, times(1)).findById(unknownId);
    }

    // ========== Tests save ==========

    @Test
    @DisplayName("Devrait sauvegarder un nouvel utilisateur")
    void shouldSaveNewUser() {
        // GIVEN - On simule que le repository retourne l'user avec un ID
        User newUser = new User("bob", "password123", "ROLE_USER");
        User savedUser = new User("bob", "password123", "ROLE_USER");
        // Simuler l'ID attribué par la base
        savedUser.setCreatedAt(LocalDateTime.now());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // WHEN
        User result = userService.save(newUser);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("bob");
        verify(userRepository, times(1)).save(newUser);
    }

    @Test
    @DisplayName("Devrait mettre à jour un utilisateur existant")
    void shouldUpdateExistingUser() {
        // GIVEN - Utilisateur existant qu'on veut modifier
        testUser.setPassword("$2a$10$newHashedPassword");

        when(userRepository.save(testUser)).thenReturn(testUser);

        // WHEN
        User result = userService.save(testUser);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getPassword()).isEqualTo("$2a$10$newHashedPassword");
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Devrait gérer la sauvegarde avec des valeurs nulles")
    void shouldHandleSaveWithNullValues() {
        // GIVEN - User avec certaines valeurs nulles
        User userWithNulls = new User();
        userWithNulls.setUsername("charlie");

        when(userRepository.save(any(User.class))).thenReturn(userWithNulls);

        // WHEN
        User result = userService.save(userWithNulls);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("charlie");
        verify(userRepository, times(1)).save(userWithNulls);
    }
}