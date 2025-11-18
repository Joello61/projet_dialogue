package com.devops.projet_dialogue.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestionnaire global des exceptions pour l'application
 * Capture toutes les exceptions non gérées et affiche une page d'erreur appropriée
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Gère les exceptions liées aux utilisateurs non trouvés
     */
    @ExceptionHandler(UserNotFoundException.class)
    public String handleUserNotFoundException(UserNotFoundException e, Model model) {
        logger.error("Utilisateur non trouvé : {}", e.getMessage());
        model.addAttribute("errorTitle", "Utilisateur introuvable");
        model.addAttribute("errorMessage", e.getMessage());
        return "error";
    }

    /**
     * Gère toutes les RuntimeException génériques
     */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException e, Model model) {
        logger.error("Erreur runtime : ", e);
        model.addAttribute("errorTitle", "Erreur");
        model.addAttribute("errorMessage", e.getMessage());
        return "error";
    }

    /**
     * Gère toutes les autres exceptions non prévues
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception e, Model model) {
        logger.error("Erreur inattendue : ", e);
        model.addAttribute("errorTitle", "Erreur inattendue");
        model.addAttribute("errorMessage", "Une erreur s'est produite. Veuillez réessayer.");
        return "error";
    }
}