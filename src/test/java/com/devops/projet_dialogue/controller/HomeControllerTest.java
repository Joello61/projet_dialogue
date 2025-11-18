package com.devops.projet_dialogue.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HomeController.class)
    @AutoConfigureMockMvc(addFilters = false)
    @DisplayName("Tests du HomeController")
    @Nested
    class HomeControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("GET / devrait afficher la page d'accueil")
        void shouldShowHomePage() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("home"));
        }

        @Test
        @DisplayName("GET / devrait être accessible sans authentification")
        void shouldBeAccessibleWithoutAuth() throws Exception {
            // Pas de @WithMockUser
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("home"));
        }

        @Test
        @WithMockUser(username = "alice")
        @DisplayName("GET / devrait fonctionner même si user connecté")
        void shouldWorkWhenAuthenticated() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("home"));
        }
    }
