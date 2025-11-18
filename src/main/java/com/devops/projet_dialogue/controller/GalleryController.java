package com.devops.projet_dialogue.controller;

import com.devops.projet_dialogue.exception.UserNotFoundException;
import com.devops.projet_dialogue.model.Conversation;
import com.devops.projet_dialogue.model.Photo;
import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.service.ConversationService;
import com.devops.projet_dialogue.service.MessageService;
import com.devops.projet_dialogue.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/conversation")
public class GalleryController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final UserService userService;

    public GalleryController(ConversationService conversationService,
                             MessageService messageService,
                             UserService userService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.userService = userService;
    }

    /**
     * Affiche la galerie de photos d'une conversation
     */
    @GetMapping("/{id}/gallery")
    public String gallery(@PathVariable Long id, Model model, Principal principal) {
        // Récupérer l'utilisateur connecté
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));

        // Récupérer la conversation
        Conversation conv = conversationService.findById(id);

        // Vérifier que l'utilisateur participe à la conversation
        if (!isUserParticipant(conv, currentUser)) {
            return "redirect:/conversations";
        }

        // Déterminer l'autre utilisateur
        User otherUser = getOtherUser(conv, currentUser);

        // Charger les photos
        List<Photo> photos = messageService.listPhotos(id);

        // Ajouter les attributs au modèle
        model.addAttribute("conversation", conv);
        model.addAttribute("photos", photos);
        model.addAttribute("otherUser", otherUser);

        return "gallery";
    }

    /**
     * Vérifie si l'utilisateur participe à la conversation
     */
    private boolean isUserParticipant(Conversation conversation, User user) {
        return conversation.getUser1().getId().equals(user.getId()) ||
                conversation.getUser2().getId().equals(user.getId());
    }

    /**
     * Retourne l'autre utilisateur de la conversation
     */
    private User getOtherUser(Conversation conversation, User currentUser) {
        return conversation.getUser1().getId().equals(currentUser.getId())
                ? conversation.getUser2()
                : conversation.getUser1();
    }
}