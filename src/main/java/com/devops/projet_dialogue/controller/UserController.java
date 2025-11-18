package com.devops.projet_dialogue.controller;

import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.repository.UserRepository;
import com.devops.projet_dialogue.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Gestion des erreurs de conversion de type (ex: ID invalide)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleTypeMismatch() {
        return "error-400";
    }

    /**
     * Page d'accueil de l'utilisateur connecté
     */
    @GetMapping("/home")
    public String userHome(Model model, Authentication auth) {
        // Vérifier l'authentification
        if (auth == null || auth.getName() == null) {
            return "redirect:/login";
        }

        model.addAttribute("username", auth.getName());
        return "user-home";
    }

    /**
     * Liste de tous les utilisateurs (sauf l'utilisateur connecté)
     */
    @GetMapping
    public String listUsers(Model model, Authentication auth) {
        // Vérifier l'authentification
        if (auth == null || auth.getPrincipal() == null) {
            return "redirect:/login";
        }

        try {
            CustomUserDetails currentUser = (CustomUserDetails) auth.getPrincipal();
            Long currentUserId = currentUser.getUser().getId();

            // Récupérer tous les utilisateurs sauf l'utilisateur connecté
            List<User> users = userRepository.findAll()
                    .stream()
                    .filter(u -> u.getId() != null && !u.getId().equals(currentUserId))
                    .collect(Collectors.toList());

            model.addAttribute("users", users);
            model.addAttribute("currentUserId", currentUserId);

            return "users/list";
        } catch (Exception e) {
            // En cas d'erreur, rediriger
            return "redirect:/login";
        }
    }

    /**
     * Voir le profil d'un autre utilisateur
     */
    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model, Authentication auth) {
        // Vérifier l'authentification
        if (auth == null || auth.getPrincipal() == null) {
            return "redirect:/login";
        }

        try {
            CustomUserDetails currentUser = (CustomUserDetails) auth.getPrincipal();
            Long currentUserId = currentUser.getUser().getId();

            // Empêcher l'utilisateur de voir son propre profil via /user/{id}
            if (id.equals(currentUserId)) {
                return "redirect:/user/home";
            }

            // Rechercher l'utilisateur cible
            User target = userRepository.findById(id).orElse(null);

            if (target == null) {
                return "redirect:/user?notfound";
            }

            model.addAttribute("user", target);
            model.addAttribute("currentUserId", currentUserId);

            return "users/view";
        } catch (Exception e) {
            // En cas d'erreur, rediriger vers la liste
            return "redirect:/user";
        }
    }
}