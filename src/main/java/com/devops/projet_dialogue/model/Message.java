package com.devops.projet_dialogue.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Conversation conversation;

    @ManyToOne(optional = false)
    private User sender;

    @OneToOne
    private Photo photo;  // optionnel

    @Column(columnDefinition = "TEXT")
    private String text;   // optionnel

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- GETTERS & SETTERS ---

    public Long getId() { return id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public Photo getPhoto() { return photo; }
    public void setPhoto(Photo photo) { this.photo = photo; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message m)) return false;
        return Objects.equals(id, m.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
