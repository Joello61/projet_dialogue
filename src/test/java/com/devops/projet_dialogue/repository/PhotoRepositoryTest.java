package com.devops.projet_dialogue.repository;

import com.devops.projet_dialogue.model.Photo;
import com.devops.projet_dialogue.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour PhotoRepository
 * Ce repository hérite seulement de JpaRepository (pas de requêtes custom)
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests du PhotoRepository")
class PhotoRepositoryTest {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        // Nettoyer la base
        photoRepository.deleteAll();

        // Créer des utilisateurs
        alice = new User("alice", "password1", "ROLE_USER");
        alice.setCreatedAt(LocalDateTime.now());

        bob = new User("bob", "password2", "ROLE_USER");
        bob.setCreatedAt(LocalDateTime.now());

        alice = entityManager.persistAndFlush(alice);
        bob = entityManager.persistAndFlush(bob);
    }

    // ========== Tests save ==========

    @Test
    @DisplayName("Devrait sauvegarder une nouvelle photo")
    void shouldSaveNewPhoto() {
        // GIVEN
        Photo photo = new Photo(
                "uuid_photo1.jpg",
                "photo1.jpg",
                "/uploads/uuid_photo1.jpg",
                alice
        );
        photo.setCreatedAt(LocalDateTime.now());

        // WHEN
        Photo saved = photoRepository.save(photo);

        // THEN
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFilename()).isEqualTo("uuid_photo1.jpg");
        assertThat(saved.getOriginalFilename()).isEqualTo("photo1.jpg");
        assertThat(saved.getUrl()).isEqualTo("/uploads/uuid_photo1.jpg");
        assertThat(saved.getAuthor()).isEqualTo(alice);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Devrait générer un ID automatiquement")
    void shouldGenerateIdAutomatically() {
        // GIVEN
        Photo photo = new Photo(
                "uuid_test.jpg",
                "test.jpg",
                "/uploads/uuid_test.jpg",
                bob
        );
        photo.setCreatedAt(LocalDateTime.now());

        assertThat(photo.getId()).isNull(); // ID null avant sauvegarde

        // WHEN
        Photo saved = photoRepository.save(photo);

        // THEN
        assertThat(saved.getId()).isNotNull(); // ID généré après sauvegarde
    }

    @Test
    @DisplayName("Devrait mettre à jour une photo existante")
    void shouldUpdateExistingPhoto() {
        // GIVEN - Sauvegarder une photo
        Photo photo = new Photo(
                "old_filename.jpg",
                "old.jpg",
                "/uploads/old_filename.jpg",
                alice
        );
        photo.setCreatedAt(LocalDateTime.now());
        Photo saved = entityManager.persistAndFlush(photo);
        Long photoId = saved.getId();

        // WHEN - Modifier le filename
        saved.setFilename("new_filename.jpg");
        saved.setUrl("/uploads/new_filename.jpg");
        photoRepository.save(saved);
        entityManager.flush();

        // THEN
        Photo updated = photoRepository.findById(photoId).orElseThrow();
        assertThat(updated.getFilename()).isEqualTo("new_filename.jpg");
        assertThat(updated.getUrl()).isEqualTo("/uploads/new_filename.jpg");
        assertThat(updated.getOriginalFilename()).isEqualTo("old.jpg"); // Inchangé
    }

    // ========== Tests findById ==========

    @Test
    @DisplayName("Devrait trouver une photo par ID")
    void shouldFindPhotoById() {
        // GIVEN
        Photo photo = new Photo(
                "uuid_find.jpg",
                "find.jpg",
                "/uploads/uuid_find.jpg",
                bob
        );
        photo.setCreatedAt(LocalDateTime.now());
        Photo saved = entityManager.persistAndFlush(photo);

        // WHEN
        Optional<Photo> result = photoRepository.findById(saved.getId());

        // THEN
        assertThat(result).isPresent();
        assertThat(result.get().getFilename()).isEqualTo("uuid_find.jpg");
        assertThat(result.get().getAuthor()).isEqualTo(bob);
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty pour ID inexistant")
    void shouldReturnEmpty_ForNonExistentId() {
        // WHEN
        Optional<Photo> result = photoRepository.findById(999L);

        // THEN
        assertThat(result).isEmpty();
    }

    // ========== Tests findAll ==========

    @Test
    @DisplayName("Devrait retourner toutes les photos")
    void shouldFindAllPhotos() {
        // GIVEN - 3 photos
        Photo photo1 = new Photo("uuid1.jpg", "photo1.jpg", "/uploads/uuid1.jpg", alice);
        photo1.setCreatedAt(LocalDateTime.now().minusDays(2));

        Photo photo2 = new Photo("uuid2.jpg", "photo2.jpg", "/uploads/uuid2.jpg", bob);
        photo2.setCreatedAt(LocalDateTime.now().minusDays(1));

        Photo photo3 = new Photo("uuid3.jpg", "photo3.jpg", "/uploads/uuid3.jpg", alice);
        photo3.setCreatedAt(LocalDateTime.now());

        entityManager.persist(photo1);
        entityManager.persist(photo2);
        entityManager.persist(photo3);
        entityManager.flush();

        // WHEN
        List<Photo> allPhotos = photoRepository.findAll();

        // THEN
        assertThat(allPhotos).hasSize(3);
        assertThat(allPhotos).extracting(Photo::getFilename)
                .containsExactlyInAnyOrder("uuid1.jpg", "uuid2.jpg", "uuid3.jpg");
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucune photo")
    void shouldReturnEmptyList_WhenNoPhotos() {
        // WHEN
        List<Photo> allPhotos = photoRepository.findAll();

        // THEN
        assertThat(allPhotos).isEmpty();
    }

    @Test
    @DisplayName("Devrait retourner les photos avec leurs auteurs")
    void shouldReturnPhotos_WithAuthors() {
        // GIVEN
        Photo photoAlice = new Photo("alice_photo.jpg", "alice.jpg", "/uploads/alice_photo.jpg", alice);
        photoAlice.setCreatedAt(LocalDateTime.now());

        Photo photoBob = new Photo("bob_photo.jpg", "bob.jpg", "/uploads/bob_photo.jpg", bob);
        photoBob.setCreatedAt(LocalDateTime.now());

        entityManager.persist(photoAlice);
        entityManager.persist(photoBob);
        entityManager.flush();

        // WHEN
        List<Photo> allPhotos = photoRepository.findAll();

        // THEN
        assertThat(allPhotos).hasSize(2);

        Photo alicePhoto = allPhotos.stream()
                .filter(p -> p.getFilename().equals("alice_photo.jpg"))
                .findFirst()
                .orElseThrow();
        assertThat(alicePhoto.getAuthor().getUsername()).isEqualTo("alice");

        Photo bobPhoto = allPhotos.stream()
                .filter(p -> p.getFilename().equals("bob_photo.jpg"))
                .findFirst()
                .orElseThrow();
        assertThat(bobPhoto.getAuthor().getUsername()).isEqualTo("bob");
    }

    // ========== Tests count ==========

    @Test
    @DisplayName("Devrait compter le nombre de photos")
    void shouldCountPhotos() {
        // GIVEN
        Photo photo1 = new Photo("uuid1.jpg", "photo1.jpg", "/uploads/uuid1.jpg", alice);
        photo1.setCreatedAt(LocalDateTime.now());

        Photo photo2 = new Photo("uuid2.jpg", "photo2.jpg", "/uploads/uuid2.jpg", bob);
        photo2.setCreatedAt(LocalDateTime.now());

        entityManager.persist(photo1);
        entityManager.persist(photo2);
        entityManager.flush();

        // WHEN
        long count = photoRepository.count();

        // THEN
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Devrait retourner 0 si aucune photo")
    void shouldReturnZero_WhenNoPhotos() {
        // WHEN
        long count = photoRepository.count();

        // THEN
        assertThat(count).isEqualTo(0);
    }

    // ========== Tests delete ==========

    @Test
    @DisplayName("Devrait supprimer une photo")
    void shouldDeletePhoto() {
        // GIVEN
        Photo photo = new Photo("uuid_delete.jpg", "delete.jpg", "/uploads/uuid_delete.jpg", alice);
        photo.setCreatedAt(LocalDateTime.now());
        Photo saved = entityManager.persistAndFlush(photo);

        assertThat(photoRepository.count()).isEqualTo(1);

        // WHEN
        photoRepository.delete(saved);
        entityManager.flush();

        // THEN
        assertThat(photoRepository.count()).isEqualTo(0);
        assertThat(photoRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Devrait supprimer une photo par ID")
    void shouldDeletePhotoById() {
        // GIVEN
        Photo photo = new Photo("uuid_delete_id.jpg", "delete_id.jpg", "/uploads/uuid_delete_id.jpg", bob);
        photo.setCreatedAt(LocalDateTime.now());
        Photo saved = entityManager.persistAndFlush(photo);
        Long photoId = saved.getId();

        // WHEN
        photoRepository.deleteById(photoId);
        entityManager.flush();

        // THEN
        assertThat(photoRepository.findById(photoId)).isEmpty();
    }

    // ========== Tests existsById ==========

    @Test
    @DisplayName("Devrait vérifier l'existence d'une photo par ID")
    void shouldCheckIfPhotoExists() {
        // GIVEN
        Photo photo = new Photo("uuid_exists.jpg", "exists.jpg", "/uploads/uuid_exists.jpg", alice);
        photo.setCreatedAt(LocalDateTime.now());
        Photo saved = entityManager.persistAndFlush(photo);

        // WHEN & THEN
        assertThat(photoRepository.existsById(saved.getId())).isTrue();
        assertThat(photoRepository.existsById(999L)).isFalse();
    }

    // ========== Tests de persistence des relations ==========

    @Test
    @DisplayName("Devrait conserver la relation avec l'auteur")
    void shouldMaintainAuthorRelationship() {
        // GIVEN
        Photo photo = new Photo("uuid_relation.jpg", "relation.jpg", "/uploads/uuid_relation.jpg", alice);
        photo.setCreatedAt(LocalDateTime.now());
        photoRepository.save(photo);
        entityManager.flush();
        entityManager.clear(); // Vider le cache pour forcer un rechargement

        // WHEN - Recharger la photo depuis la base
        Photo reloaded = photoRepository.findById(photo.getId()).orElseThrow();

        // THEN - L'auteur doit être correctement chargé
        assertThat(reloaded.getAuthor()).isNotNull();
        assertThat(reloaded.getAuthor().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("Devrait gérer les noms de fichiers avec caractères spéciaux")
    void shouldHandleSpecialCharacters() {
        // GIVEN
        Photo photo = new Photo(
                "uuid_été_2024_(1).jpg",
                "photo été 2024 (1).jpg",
                "/uploads/uuid_été_2024_(1).jpg",
                bob
        );
        photo.setCreatedAt(LocalDateTime.now());

        // WHEN
        Photo saved = photoRepository.save(photo);

        // THEN
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOriginalFilename()).isEqualTo("photo été 2024 (1).jpg");

        // Vérifier qu'on peut la retrouver
        Optional<Photo> found = photoRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getOriginalFilename()).isEqualTo("photo été 2024 (1).jpg");
    }
}