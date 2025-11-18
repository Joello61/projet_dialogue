package com.devops.projet_dialogue.controller;

import com.devops.projet_dialogue.dto.RegisterRequest;
import com.devops.projet_dialogue.model.User;
import com.devops.projet_dialogue.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult bindingResult,
                                 Model model) {

        // Vérifier les erreurs de validation (@NotBlank, @Size, etc.)
        if (bindingResult.hasErrors()) {
            return "register";
        }

        // Vérifier que les mots de passe correspondent
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("errorMessage", "Les mots de passe ne correspondent pas.");
            return "register";
        }

        // Vérifier que le username n'existe pas déjà
        if (userRepository.existsByUsername(request.getUsername())) {
            model.addAttribute("errorMessage", "Ce nom d'utilisateur est déjà pris.");
            return "register";
        }

        // Créer et sauvegarder l'utilisateur
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");

        userRepository.save(user);

        // Redirection vers le login après inscription réussie
        return "redirect:/login?registered";
    }
}