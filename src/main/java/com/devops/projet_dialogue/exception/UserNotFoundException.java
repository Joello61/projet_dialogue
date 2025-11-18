package com.devops.projet_dialogue.exception;

/**
 * Exception levée lorsqu'un utilisateur n'est pas trouvé en base de données
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String username) {
        super("Utilisateur non trouvé : " + username);
    }

    public UserNotFoundException(Long userId) {
        super("Utilisateur non trouvé avec l'ID : " + userId);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}