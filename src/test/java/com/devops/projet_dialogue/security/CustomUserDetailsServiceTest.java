package com.devops.projet_dialogue.security;

import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour CustomUserDetailsService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User("alice", "$2a$10$hashedPassword", "ROLE_USER");
        alice.setCreatedAt(LocalDateTime.now());
    }

    // ========== Tests loadUserByUsername ==========

    @Test
    @DisplayName("Devrait charger un utilisateur par username")
    void shouldLoadUserByUsername() {
        // GIVEN
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        // WHEN
        UserDetails userDetails = userDetailsService.loadUserByUsername("alice");

        // THEN
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("alice");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword");

        verify(userRepository, times(1)).findByUsername("alice");
    }

    @Test
    @DisplayName("Devrait retourner les authorities correctement")
    void shouldReturnAuthorities_Correctly() {
        // GIVEN
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        // WHEN
        UserDetails userDetails = userDetailsService.loadUserByUsername("alice");

        // THEN
        assertThat(userDetails.getAuthorities()).hasSize(1);

        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Devrait lever UsernameNotFoundException si user inexistant")
    void shouldThrowException_WhenUserNotFound() {
        // GIVEN
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Utilisateur non trouvé : unknown");

        verify(userRepository, times(1)).findByUsername("unknown");
    }

    @Test
    @DisplayName("Devrait gérer différents rôles")
    void shouldHandleDifferentRoles() {
        // GIVEN - Utilisateur ADMIN
        User admin = new User("admin", "password", "ROLE_ADMIN");
        admin.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        // WHEN
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        // THEN
        assertThat(userDetails.getAuthorities()).hasSize(1);

        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Devrait retourner isEnabled = true")
    void shouldReturnEnabled_True() {
        // GIVEN
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        // WHEN
        UserDetails userDetails = userDetailsService.loadUserByUsername("alice");

        // THEN
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Devrait gérer les usernames avec espaces")
    void shouldHandleUsernamesWithSpaces() {
        // GIVEN
        User userWithSpaces = new User("user name", "password", "ROLE_USER");
        userWithSpaces.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("user name")).thenReturn(Optional.of(userWithSpaces));

        // WHEN
        UserDetails userDetails = userDetailsService.loadUserByUsername("user name");

        // THEN
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("user name");
    }

    @Test
    @DisplayName("Devrait être case-sensitive pour les usernames")
    void shouldBeCaseSensitive_ForUsernames() {
        // GIVEN
        when(userRepository.findByUsername("Alice")).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("Alice"))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository, times(1)).findByUsername("Alice");
    }

    @Test
    @DisplayName("Devrait gérer les usernames vides")
    void shouldHandleEmptyUsername() {
        // GIVEN
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Devrait gérer les usernames null")
    void shouldHandleNullUsername() {
        // GIVEN
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Devrait charger plusieurs utilisateurs différents")
    void shouldLoadMultipleDifferentUsers() {
        // GIVEN
        User bob = new User("bob", "password", "ROLE_USER");
        bob.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        // WHEN
        UserDetails aliceDetails = userDetailsService.loadUserByUsername("alice");
        UserDetails bobDetails = userDetailsService.loadUserByUsername("bob");

        // THEN
        assertThat(aliceDetails.getUsername()).isEqualTo("alice");
        assertThat(bobDetails.getUsername()).isEqualTo("bob");

        verify(userRepository, times(1)).findByUsername("alice");
        verify(userRepository, times(1)).findByUsername("bob");
    }
}