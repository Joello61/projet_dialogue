package com.devops.projet_dialogue.controller;

import com.devops.projet_dialogue.exception.UserNotFoundException;
import com.devops.projet_dialogue.model.Conversation;
import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.service.ConversationService;
import com.devops.projet_dialogue.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final UserService userService;

    public ConversationController(ConversationService conversationService,
                                  UserService userService) {
        this.conversationService = conversationService;
        this.userService = userService;
    }

    /**
     * Liste des conversations de l'utilisateur connecté
     */
    @GetMapping
    public String listConversations(Model model, Principal principal) {
        // Récupérer l'utilisateur connecté
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));

        // Récupérer toutes ses conversations
        List<Conversation> conversations = conversationService.findAllForUser(currentUser.getId());

        model.addAttribute("conversations", conversations);
        model.addAttribute("currentUser", currentUser);

        return "conversations"; // templates/conversations.html
    }

    /**
     * Ouvrir une conversation avec un utilisateur (ou la créer si elle n'existe pas)
     */
    @GetMapping("/with/{userId}")
    public String openConversation(@PathVariable Long userId, Principal principal) {
        // Récupérer l'utilisateur connecté
        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException(principal.getName()));

        // Créer ou récupérer la conversation
        Conversation conversation = conversationService.getOrCreate(
                currentUser.getId(),
                userId
        );

        // Rediriger vers la page de la conversation
        return "redirect:/conversation/" + conversation.getId();
    }
}