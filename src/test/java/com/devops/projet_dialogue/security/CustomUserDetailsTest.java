package com.devops.projet_dialogue.security;

import com.devops.projet_dialogue.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour CustomUserDetails
 */
@DisplayName("Tests du CustomUserDetails")
class CustomUserDetailsTest {

    private User alice;
    private CustomUserDetails aliceDetails;

    @BeforeEach
    void setUp() {
        alice = new User("alice", "$2a$10$hashedPassword", "ROLE_USER");
        alice.setCreatedAt(LocalDateTime.now());

        aliceDetails = new CustomUserDetails(alice);
    }

    // ========== Tests getAuthorities ==========

    @Test
    @DisplayName("Devrait retourner les authorities du user")
    void shouldReturnAuthorities() {
        // WHEN
        Collection<? extends GrantedAuthority> authorities = aliceDetails.getAuthorities();

        // THEN
        assertThat(authorities).hasSize(1);

        GrantedAuthority authority = authorities.iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Devrait gérer le rôle ROLE_ADMIN")
    void shouldHandleAdminRole() {
        // GIVEN
        User admin = new User("admin", "password", "ROLE_ADMIN");
        admin.setCreatedAt(LocalDateTime.now());

        CustomUserDetails adminDetails = new CustomUserDetails(admin);

        // WHEN
        Collection<? extends GrantedAuthority> authorities = adminDetails.getAuthorities();

        // THEN
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Devrait gérer un rôle custom")
    void shouldHandleCustomRole() {
        // GIVEN
        User moderator = new User("mod", "password", "ROLE_MODERATOR");
        moderator.setCreatedAt(LocalDateTime.now());

        CustomUserDetails modDetails = new CustomUserDetails(moderator);

        // WHEN
        Collection<? extends GrantedAuthority> authorities = modDetails.getAuthorities();

        // THEN
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_MODERATOR");
    }

    // ========== Tests getPassword ==========

    @Test
    @DisplayName("Devrait retourner le mot de passe encodé")
    void shouldReturnEncodedPassword() {
        // WHEN
        String password = aliceDetails.getPassword();

        // THEN
        assertThat(password).isEqualTo("$2a$10$hashedPassword");
    }

    @Test
    @DisplayName("Devrait retourner le mot de passe même s'il est vide")
    void shouldReturnEmptyPassword() {
        // GIVEN
        User userWithEmptyPassword = new User("user", "", "ROLE_USER");
        userWithEmptyPassword.setCreatedAt(LocalDateTime.now());

        CustomUserDetails details = new CustomUserDetails(userWithEmptyPassword);

        // WHEN
        String password = details.getPassword();

        // THEN
        assertThat(password).isEmpty();
    }

    // ========== Tests getUsername ==========

    @Test
    @DisplayName("Devrait retourner le username")
    void shouldReturnUsername() {
        // WHEN
        String username = aliceDetails.getUsername();

        // THEN
        assertThat(username).isEqualTo("alice");
    }

    @Test
    @DisplayName("Devrait gérer les usernames avec espaces")
    void shouldHandleUsernamesWithSpaces() {
        // GIVEN
        User userWithSpaces = new User("user name", "password", "ROLE_USER");
        userWithSpaces.setCreatedAt(LocalDateTime.now());

        CustomUserDetails details = new CustomUserDetails(userWithSpaces);

        // WHEN
        String username = details.getUsername();

        // THEN
        assertThat(username).isEqualTo("user name");
    }

    // ========== Tests isAccountNonExpired ==========

    @Test
    @DisplayName("Le compte ne devrait jamais être expiré")
    void shouldReturnAccountNonExpired_True() {
        // WHEN
        boolean isNonExpired = aliceDetails.isAccountNonExpired();

        // THEN
        assertThat(isNonExpired).isTrue();
    }

    // ========== Tests isAccountNonLocked ==========

    @Test
    @DisplayName("Le compte ne devrait jamais être verrouillé")
    void shouldReturnAccountNonLocked_True() {
        // WHEN
        boolean isNonLocked = aliceDetails.isAccountNonLocked();

        // THEN
        assertThat(isNonLocked).isTrue();
    }

    // ========== Tests isCredentialsNonExpired ==========

    @Test
    @DisplayName("Les credentials ne devraient jamais expirer")
    void shouldReturnCredentialsNonExpired_True() {
        // WHEN
        boolean isNonExpired = aliceDetails.isCredentialsNonExpired();

        // THEN
        assertThat(isNonExpired).isTrue();
    }

    // ========== Tests isEnabled ==========

    @Test
    @DisplayName("Le compte devrait toujours être activé")
    void shouldReturnEnabled_True() {
        // WHEN
        boolean isEnabled = aliceDetails.isEnabled();

        // THEN
        assertThat(isEnabled).isTrue();
    }

    // ========== Tests getUser ==========

    @Test
    @DisplayName("Devrait retourner l'objet User original")
    void shouldReturnOriginalUser() {
        // WHEN
        User user = aliceDetails.getUser();

        // THEN
        assertThat(user).isEqualTo(alice);
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("L'objet User retourné devrait être le même (référence)")
    void shouldReturnSameUserReference() {
        // WHEN
        User user1 = aliceDetails.getUser();
        User user2 = aliceDetails.getUser();

        // THEN
        assertThat(user1).isSameAs(user2);
        assertThat(user1).isSameAs(alice);
    }

    // ========== Tests de cohérence ==========

    @Test
    @DisplayName("Toutes les méthodes devraient retourner des valeurs cohérentes")
    void shouldReturnConsistentValues() {
        // WHEN & THEN
        assertThat(aliceDetails.getUsername()).isEqualTo(alice.getUsername());
        assertThat(aliceDetails.getPassword()).isEqualTo(alice.getPassword());
        assertThat(aliceDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo(alice.getRole());
    }

    // ========== Tests avec différents types d'utilisateurs ==========

    @Test
    @DisplayName("Devrait fonctionner avec un utilisateur avec username long")
    void shouldWorkWithLongUsername() {
        // GIVEN
        String longUsername = "a".repeat(50);
        User userLongName = new User(longUsername, "password", "ROLE_USER");
        userLongName.setCreatedAt(LocalDateTime.now());

        CustomUserDetails details = new CustomUserDetails(userLongName);

        // WHEN & THEN
        assertThat(details.getUsername()).isEqualTo(longUsername);
        assertThat(details.getUsername()).hasSize(50);
    }

    @Test
    @DisplayName("Devrait fonctionner avec différents mots de passe encodés")
    void shouldWorkWithDifferentEncodedPasswords() {
        // GIVEN - Différents formats de mot de passe
        String[] encodedPasswords = {
                "$2a$10$hashedPassword1",
                "$2a$12$differentHash",
                "$2b$10$anotherHash",
                "plainTextPassword" // Pas recommandé mais testé
        };

        for (String encodedPassword : encodedPasswords) {
            User user = new User("user", encodedPassword, "ROLE_USER");
            user.setCreatedAt(LocalDateTime.now());

            CustomUserDetails details = new CustomUserDetails(user);

            // WHEN & THEN
            assertThat(details.getPassword()).isEqualTo(encodedPassword);
        }
    }

    @Test
    @DisplayName("Devrait gérer les rôles sans préfixe ROLE_")
    void shouldHandleRolesWithoutPrefix() {
        // GIVEN - Rôle sans préfixe ROLE_
        User userBadRole = new User("user", "password", "USER");
        userBadRole.setCreatedAt(LocalDateTime.now());

        CustomUserDetails details = new CustomUserDetails(userBadRole);

        // WHEN
        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

        // THEN - Devrait quand même fonctionner
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Tous les flags de statut devraient être true")
    void shouldHaveAllStatusFlagsTrue() {
        // WHEN & THEN
        assertThat(aliceDetails.isAccountNonExpired()).isTrue();
        assertThat(aliceDetails.isAccountNonLocked()).isTrue();
        assertThat(aliceDetails.isCredentialsNonExpired()).isTrue();
        assertThat(aliceDetails.isEnabled()).isTrue();
    }
}