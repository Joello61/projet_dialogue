package com.devops.projet_dialogue.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "photos")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom du fichier enregistré dans /uploads
    @Column(nullable = false)
    private String filename;

    // Nom original du fichier envoyé par l’utilisateur
    private String originalFilename;

    // URL publique d’accès à la photo
    @Column(nullable = false)
    private String url;

    // L'auteur de la photo
    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    // Date de publication
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // ========= CONSTRUCTEURS =========

    public Photo() {
    }

    public Photo(String filename, String originalFilename, String url, User author) {
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.url = url;
        this.author = author;
        this.createdAt = LocalDateTime.now();
    }

    // ========= GETTERS & SETTERS =========

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Photo photo)) return false;
        return Objects.equals(id, photo.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}

