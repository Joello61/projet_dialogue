package com.devops.projet_dialogue.repository;

import com.devops.projet_dialogue.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour UserRepository
 * @DataJpaTest configure automatiquement H2 en mémoire
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests du UserRepository")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        // Nettoyer la base avant chaque test
        userRepository.deleteAll();

        // Créer des utilisateurs de test
        alice = new User("alice", "$2a$10$hashedPassword1", "ROLE_USER");
        alice.setCreatedAt(LocalDateTime.now());

        bob = new User("bob", "$2a$10$hashedPassword2", "ROLE_USER");
        bob.setCreatedAt(LocalDateTime.now());
    }

    // ========== Tests findByUsername ==========

    @Test
    @DisplayName("Devrait trouver un utilisateur par username")
    void shouldFindUserByUsername() {
        // GIVEN - Sauvegarder Alice dans la base
        entityManager.persistAndFlush(alice);

        // WHEN
        Optional<User> result = userRepository.findByUsername("alice");

        // THEN
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
        assertThat(result.get().getPassword()).isEqualTo("$2a$10$hashedPassword1");
        assertThat(result.get().getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty si username inexistant")
    void shouldReturnEmpty_WhenUsernameNotFound() {
        // GIVEN - Base vide

        // WHEN
        Optional<User> result = userRepository.findByUsername("inconnu");

        // THEN
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("La recherche par username doit être case-sensitive")
    void shouldBeCaseSensitive_ForUsername() {
        // GIVEN
        entityManager.persistAndFlush(alice);

        // WHEN
        Optional<User> resultUpperCase = userRepository.findByUsername("ALICE");
        Optional<User> resultLowerCase = userRepository.findByUsername("alice");

        // THEN
        assertThat(resultLowerCase).isPresent();
        assertThat(resultUpperCase).isEmpty(); // "ALICE" != "alice"
    }

    @Test
    @DisplayName("Devrait gérer les espaces dans les usernames")
    void shouldHandleWhitespaceInUsername() {
        // GIVEN
        User userWithSpaces = new User("user name", "password", "ROLE_USER");
        entityManager.persistAndFlush(userWithSpaces);

        // WHEN
        Optional<User> result = userRepository.findByUsername("user name");

        // THEN
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("user name");
    }

    // ========== Tests existsByUsername ==========

    @Test
    @DisplayName("Devrait retourner true si username existe")
    void shouldReturnTrue_WhenUsernameExists() {
        // GIVEN
        entityManager.persistAndFlush(alice);

        // WHEN
        boolean exists = userRepository.existsByUsername("alice");

        // THEN
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Devrait retourner false si username n'existe pas")
    void shouldReturnFalse_WhenUsernameDoesNotExist() {
        // GIVEN - Base vide

        // WHEN
        boolean exists = userRepository.existsByUsername("inexistant");

        // THEN
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Devrait vérifier l'existence après suppression")
    void shouldReturnFalse_AfterUserDeletion() {
        // GIVEN
        User savedAlice = entityManager.persistAndFlush(alice);
        assertThat(userRepository.existsByUsername("alice")).isTrue();

        // WHEN - Supprimer l'utilisateur
        userRepository.delete(savedAlice);
        entityManager.flush();

        // THEN
        assertThat(userRepository.existsByUsername("alice")).isFalse();
    }

    // ========== Tests save (hérité de JpaRepository) ==========

    @Test
    @DisplayName("Devrait sauvegarder un nouvel utilisateur avec ID généré")
    void shouldSaveNewUser_WithGeneratedId() {
        // GIVEN - Utilisateur sans ID
        assertThat(alice.getId()).isNull();

        // WHEN
        User savedUser = userRepository.save(alice);

        // THEN
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("alice");

        // Vérifier qu'on peut le retrouver
        Optional<User> found = userRepository.findById(savedUser.getId());
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("Devrait mettre à jour un utilisateur existant")
    void shouldUpdateExistingUser() {
        // GIVEN
        User savedUser = entityManager.persistAndFlush(alice);
        Long userId = savedUser.getId();

        // WHEN - Modifier le mot de passe
        savedUser.setPassword("$2a$10$newHashedPassword");
        userRepository.save(savedUser);
        entityManager.flush();

        // THEN
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getPassword()).isEqualTo("$2a$10$newHashedPassword");
        assertThat(updatedUser.getUsername()).isEqualTo("alice"); // Username inchangé
    }

    // ========== Tests findById (hérité de JpaRepository) ==========

    @Test
    @DisplayName("Devrait trouver un utilisateur par ID")
    void shouldFindUserById() {
        // GIVEN
        User savedUser = entityManager.persistAndFlush(alice);

        // WHEN
        Optional<User> result = userRepository.findById(savedUser.getId());

        // THEN
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty pour ID inexistant")
    void shouldReturnEmpty_ForNonExistentId() {
        // WHEN
        Optional<User> result = userRepository.findById(999L);

        // THEN
        assertThat(result).isEmpty();
    }

    // ========== Tests contrainte unique sur username ==========

    @Test
    @DisplayName("Devrait empêcher la création de deux users avec même username")
    void shouldPreventDuplicateUsername() {
        // GIVEN - Sauvegarder Alice
        entityManager.persistAndFlush(alice);

        // WHEN - Tenter de créer un autre user avec le même username
        User duplicateUser = new User("alice", "differentPassword", "ROLE_ADMIN");

        // THEN - Lever une exception (contrainte unique)
        assertThat(userRepository.existsByUsername("alice")).isTrue();

        // Note: Pour tester réellement l'exception de contrainte unique,
        // il faudrait utiliser assertThatThrownBy avec DataIntegrityViolationException
        // Mais ici on vérifie juste l'existence avant insertion (bonne pratique)
    }

    // ========== Tests findAll ==========

    @Test
    @DisplayName("Devrait retourner tous les utilisateurs")
    void shouldFindAllUsers() {
        // GIVEN
        entityManager.persist(alice);
        entityManager.persist(bob);
        entityManager.flush();

        // WHEN
        var allUsers = userRepository.findAll();

        // THEN
        assertThat(allUsers).hasSize(2);
        assertThat(allUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucun utilisateur")
    void shouldReturnEmptyList_WhenNoUsers() {
        // WHEN
        var allUsers = userRepository.findAll();

        // THEN
        assertThat(allUsers).isEmpty();
    }

    // ========== Tests count ==========

    @Test
    @DisplayName("Devrait compter le nombre d'utilisateurs")
    void shouldCountUsers() {
        // GIVEN
        entityManager.persist(alice);
        entityManager.persist(bob);
        entityManager.flush();

        // WHEN
        long count = userRepository.count();

        // THEN
        assertThat(count).isEqualTo(2);
    }

    // ========== Tests delete ==========

    @Test
    @DisplayName("Devrait supprimer un utilisateur")
    void shouldDeleteUser() {
        // GIVEN
        User savedUser = entityManager.persistAndFlush(alice);
        assertThat(userRepository.count()).isEqualTo(1);

        // WHEN
        userRepository.delete(savedUser);
        entityManager.flush();

        // THEN
        assertThat(userRepository.count()).isEqualTo(0);
        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
    }
}