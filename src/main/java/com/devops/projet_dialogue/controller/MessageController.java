package com.devops.projet_dialogue.controller;

import com.devops.projet_dialogue.model.Conversation;
import com.devops.projet_dialogue.model.Message;
import com.devops.projet_dialogue.model.Photo;
import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/conversation")
public class MessageController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final UserService userService;
    private final PhotoService photoService;

    public MessageController(ConversationService conversationService,
                             MessageService messageService,
                             UserService userService,
                             PhotoService photoService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.userService = userService;
        this.photoService = photoService;
    }

    /**
     * Afficher une conversation avec ses messages
     */
    @GetMapping("/{id}")
    public String viewConversation(@PathVariable Long id, Model model, Principal principal) {
        // Vérifier l'authentification
        if (principal == null) {
            return "redirect:/login";
        }

        // Récupérer l'utilisateur courant
        User currentUser = userService.findByUsername(principal.getName()).orElse(null);
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Récupérer la conversation
        Conversation conv = conversationService.findById(id);
        if (conv == null) {
            return "redirect:/conversations";
        }

        // Sécurité : vérifier que l'utilisateur participe à la conversation
        if (!conv.getUser1().getUsername().equals(currentUser.getUsername()) &&
                !conv.getUser2().getUsername().equals(currentUser.getUsername())) {
            return "redirect:/conversations";
        }

        // Trouver l'autre utilisateur de la conversation
        User otherUser = conv.getUser1().getUsername().equals(currentUser.getUsername())
                ? conv.getUser2()
                : conv.getUser1();

        // Récupérer les messages
        List<Message> messages = messageService.listMessages(id);

        // Ajouter les attributs au modèle
        model.addAttribute("conversation", conv);
        model.addAttribute("messages", messages);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherUser", otherUser);

        return "conversation";
    }

    /**
     * Envoi d'un message (texte et/ou photo)
     */
    @PostMapping("/{id}/send")
    public String sendMessage(@PathVariable Long id,
                              @RequestParam(required = false) String text,
                              @RequestParam(required = false) MultipartFile image,
                              Principal principal) {
        // Vérifier l'authentification
        if (principal == null) {
            return "redirect:/login";
        }

        // Récupérer l'utilisateur courant
        User sender = userService.findByUsername(principal.getName()).orElse(null);
        if (sender == null) {
            return "redirect:/login";
        }

        Photo savedPhoto = null;

        // Gérer la photo si présente
        try {
            if (image != null && !image.isEmpty() && !Objects.requireNonNull(image.getOriginalFilename()).isEmpty()) {
                savedPhoto = photoService.savePhoto(image, sender);
            }
        } catch (Exception e) {
            // En cas d'erreur de sauvegarde de la photo, on continue sans photo
            System.err.println("Erreur lors de la sauvegarde de la photo: " + e.getMessage());
            e.printStackTrace();
        }

        // Envoyer le message (texte et/ou photo)
        messageService.sendMessage(id, sender, text, savedPhoto);

        return "redirect:/conversation/" + id;
    }
}