package com.devops.projet_dialogue.service;

import com.devops.projet_dialogue.model.Photo;
import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.repository.PhotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class PhotoService {

    private final PhotoRepository photoRepository;

    // Chemin où stocker les fichiers (ex: project/uploads)
    private final Path uploadDir = Paths.get("uploads");

    public PhotoService(PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;

        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier 'uploads'", e);
        }
    }

    /**
     * Sauvegarde une photo uploadée par un utilisateur.
     */
    public Photo savePhoto(MultipartFile file, User author) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }

        // Vérification du type MIME
        if (!Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier n'est pas une image.");
        }

        // Nom unique pour éviter les collisions
        String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        Path destination = uploadDir.resolve(uniqueName);

        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        String url = "/uploads/" + uniqueName;

        Photo photo = new Photo(
                uniqueName,
                file.getOriginalFilename(),
                url,
                author
        );

        photo.setCreatedAt(LocalDateTime.now());

        return photoRepository.save(photo);
    }
}
