package com.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanLoginAndListResources() throws Exception {
        String token = loginAndGetToken("admin", "Admin@123");
        mockMvc.perform(get("/api/resources").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotCreateResource() throws Exception {
        String token = loginAndGetToken("user", "User@123");
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "New Room", "type", "ROOM", "description", "test", "available", true));

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanCreateAndSeeOnlyOwnReservations() throws Exception {
        String userToken = loginAndGetToken("user", "User@123");

        String reservationBody = objectMapper.writeValueAsString(Map.of(
                "resourceId", 1,
                "startTime", "2026-09-01T10:00:00",
                "endTime", "2026-09-01T12:00:00",
                "price", 25.50
        ));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(reservationBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user"));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("user"))));
    }
}
