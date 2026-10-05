package com.cricketscore.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CricketApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void managesMatchScoringInningsAndRoster() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();
        JsonNode initial = objectMapper.readTree(listResult.getResponse().getContentAsString()).get(0);
        long matchId = initial.get("id").asLong();
        long battingTeamId = initial.get("battingTeam").get("id").asLong();
        long bowlingTeamId = initial.get("bowlingTeam").get("id").asLong();
        long previousRuns = initial.get("runs").asLong();
        long previousBalls = initial.get("legalBalls").asLong();

        JsonNode batters = objectMapper.readTree(mockMvc.perform(get("/api/teams/{id}/players", battingTeamId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode bowlers = objectMapper.readTree(mockMvc.perform(get("/api/teams/{id}/players", bowlingTeamId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Map<String, Object> delivery = Map.of(
                "strikerId", batters.get(0).get("id").asLong(),
                "nonStrikerId", batters.get(1).get("id").asLong(),
                "bowlerId", bowlers.get(0).get("id").asLong(),
                "batterRuns", 4,
                "extras", 0,
                "extraType", "NONE",
                "wicket", false);

        mockMvc.perform(post("/api/matches/{id}/events", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(delivery)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runs").value(previousRuns + 4))
                .andExpect(jsonPath("$.legalBalls").value(previousBalls + 1))
                .andExpect(jsonPath("$.recentEvents", hasSize(12)));

        mockMvc.perform(post("/api/matches/{id}/innings/next", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentInnings").value(2))
                .andExpect(jsonPath("$.target").value(previousRuns + 5));

        mockMvc.perform(post("/api/matches/{id}/finish", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        String teamBody = objectMapper.writeValueAsString(Map.of("name", "Test XI", "shortName", "TST"));
        MvcResult teamResult = mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON).content(teamBody))
                .andExpect(status().isCreated())
                .andReturn();
        long createdTeamId = objectMapper.readTree(teamResult.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/teams/{id}/players", createdTeamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Batter\",\"role\":\"Batter\",\"jerseyNumber\":18}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Batter"));
    }
}