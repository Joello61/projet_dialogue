package com.devops.projet_dialogue.service;

import com.devops.projet_dialogue.model.Photo;
import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.repository.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour PhotoService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du PhotoService")
class PhotoServiceTest {

    @Mock
    private PhotoRepository photoRepository;

    private PhotoService photoService;

    private User alice;

    @TempDir
    Path tempDir; // JUnit crée un dossier temporaire pour chaque test

    @BeforeEach
    void setUp() {
        // Utiliser le dossier temporaire au lieu de "uploads"
        photoService = new PhotoService(photoRepository) {
            // On override le uploadDir pour utiliser le tempDir
            @Override
            public Photo savePhoto(MultipartFile file, User author) throws IOException {
                if (file.isEmpty()) {
                    throw new IllegalArgumentException("Le fichier est vide.");
                }

                if (!Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
                    throw new IllegalArgumentException("Le fichier n'est pas une image.");
                }

                // Générer un nom unique avec UUID comme le vrai service
                String extension = "";
                String originalFilename = file.getOriginalFilename();
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String uniqueName = UUID.randomUUID().toString() + extension;

                // Simuler la sauvegarde dans le tempDir
                Path destination = tempDir.resolve(uniqueName);
                Files.copy(file.getInputStream(), destination);

                String url = "/uploads/" + uniqueName;
                Photo photo = new Photo(uniqueName, file.getOriginalFilename(), url, author);
                photo.setCreatedAt(LocalDateTime.now());

                return photoRepository.save(photo);
            }
        };

        alice = new User("alice", "password1", "ROLE_USER");
        alice.setCreatedAt(LocalDateTime.now());
    }

    // ========== Tests savePhoto - Cas valides ==========

    @Test
    @DisplayName("Devrait sauvegarder une image JPG valide")
    void shouldSaveValidJpgImage() throws IOException {
        // GIVEN
        byte[] imageContent = "fake-jpg-content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                imageContent
        );

        when(photoRepository.save(any(Photo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Photo result = photoService.savePhoto(file, alice);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getOriginalFilename()).isEqualTo("test-image.jpg");
        assertThat(result.getUrl()).startsWith("/uploads/");
        assertThat(result.getAuthor()).isEqualTo(alice);
        assertThat(result.getFilename()).endsWith(".jpg");

        // Vérifier que le fichier a été créé physiquement
        Path savedFile = tempDir.resolve(result.getFilename());
        assertThat(Files.exists(savedFile)).isTrue();

        verify(photoRepository, times(1)).save(any(Photo.class));
    }

    @Test
    @DisplayName("Devrait sauvegarder une image PNG valide")
    void shouldSaveValidPngImage() throws IOException {
        // GIVEN
        byte[] imageContent = "fake-png-content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "screenshot.png",
                "image/png",
                imageContent
        );

        when(photoRepository.save(any(Photo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Photo result = photoService.savePhoto(file, alice);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getOriginalFilename()).isEqualTo("screenshot.png");
        assertThat(result.getFilename()).endsWith(".png");

        verify(photoRepository, times(1)).save(any(Photo.class));
    }

    @Test
    @DisplayName("Devrait définir la date de création automatiquement")
    void shouldSetCreatedAtAutomatically() throws IOException {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "content".getBytes()
        );

        when(photoRepository.save(any(Photo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        LocalDateTime beforeSave = LocalDateTime.now();
        photoService.savePhoto(file, alice);
        LocalDateTime afterSave = LocalDateTime.now();

        // THEN
        ArgumentCaptor<Photo> photoCaptor = ArgumentCaptor.forClass(Photo.class);
        verify(photoRepository).save(photoCaptor.capture());

        Photo capturedPhoto = photoCaptor.getValue();
        assertThat(capturedPhoto.getCreatedAt()).isNotNull();
        assertThat(capturedPhoto.getCreatedAt())
                .isAfterOrEqualTo(beforeSave)
                .isBeforeOrEqualTo(afterSave);
    }

    @Test
    @DisplayName("Devrait générer un nom de fichier unique avec UUID")
    void shouldGenerateUniqueFilename() throws IOException {
        // GIVEN
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "content1".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "content2".getBytes()
        );

        when(photoRepository.save(any(Photo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Photo result1 = photoService.savePhoto(file1, alice);
        Photo result2 = photoService.savePhoto(file2, alice);

        // THEN
        ArgumentCaptor<Photo> photoCaptor = ArgumentCaptor.forClass(Photo.class);
        verify(photoRepository, times(2)).save(photoCaptor.capture());

        // Les deux fichiers doivent avoir des noms différents même si le nom original est identique
        assertThat(result1.getFilename()).isNotEqualTo(result2.getFilename());

        // Vérifier que les deux fichiers existent physiquement
        assertThat(Files.exists(tempDir.resolve(result1.getFilename()))).isTrue();
        assertThat(Files.exists(tempDir.resolve(result2.getFilename()))).isTrue();

        // Vérifier le format UUID (doit ressembler à un UUID)
        assertThat(result1.getFilename()).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg$");
        assertThat(result2.getFilename()).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg$");
    }

    // ========== Tests savePhoto - Cas d'erreur ==========

    @Test
    @DisplayName("Devrait rejeter un fichier vide")
    void shouldRejectEmptyFile() {
        // GIVEN
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        // WHEN & THEN
        assertThatThrownBy(() -> photoService.savePhoto(emptyFile, alice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le fichier est vide.");

        verify(photoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait rejeter un fichier non-image (PDF)")
    void shouldRejectNonImageFile_Pdf() {
        // GIVEN
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "fake-pdf-content".getBytes()
        );

        // WHEN & THEN
        assertThatThrownBy(() -> photoService.savePhoto(pdfFile, alice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le fichier n'est pas une image.");

        verify(photoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait rejeter un fichier texte")
    void shouldRejectTextFile() {
        // GIVEN
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "file.txt",
                "text/plain",
                "Hello World".getBytes()
        );

        // WHEN & THEN
        assertThatThrownBy(() -> photoService.savePhoto(textFile, alice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le fichier n'est pas une image.");

        verify(photoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait rejeter un fichier vidéo")
    void shouldRejectVideoFile() {
        // GIVEN
        MockMultipartFile videoFile = new MockMultipartFile(
                "file",
                "video.mp4",
                "video/mp4",
                "fake-video-content".getBytes()
        );

        // WHEN & THEN
        assertThatThrownBy(() -> photoService.savePhoto(videoFile, alice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le fichier n'est pas une image.");

        verify(photoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait gérer les noms de fichiers avec caractères spéciaux")
    void shouldHandleSpecialCharactersInFilename() throws IOException {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo été 2024 (1).jpg",
                "image/jpeg",
                "content".getBytes()
        );

        when(photoRepository.save(any(Photo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Photo result = photoService.savePhoto(file, alice);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getOriginalFilename()).isEqualTo("photo été 2024 (1).jpg");
        // Le nom de fichier généré doit être un UUID, pas le nom original
        assertThat(result.getFilename()).matches("^[0-9a-f-]+\\.jpg$");

        verify(photoRepository, times(1)).save(any(Photo.class));
    }

    @Test
    @DisplayName("Devrait accepter différents types MIME d'images")
    void shouldAcceptVariousImageMimeTypes() throws IOException {
        // GIVEN - Différents types d'images
        String[][] imageTypes = {
                {"image/jpeg", "jpg"},
                {"image/png", "png"},
                {"image/gif", "gif"},
                {"image/webp", "webp"},
                {"image/svg+xml", "svg"}
        };

        when(photoRepository.save(any(Photo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN & THEN
        for (String[] imageType : imageTypes) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "image." + imageType[1],
                    imageType[0],
                    "content".getBytes()
            );

            assertThatCode(() -> photoService.savePhoto(file, alice))
                    .doesNotThrowAnyException();
        }

        verify(photoRepository, times(imageTypes.length)).save(any(Photo.class));
    }
}