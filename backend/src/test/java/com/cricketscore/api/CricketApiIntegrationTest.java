package com.cricketscore.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cricketscore.model.MatchEvent;
import com.cricketscore.repository.MatchEventRepository;
import com.cricketscore.repository.PlayerRepository;
import java.util.List;
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

        @Autowired
        private MatchEventRepository matchEvents;

        @Autowired
        private PlayerRepository players;

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
        long previousOverBowlerId = initial.get("bowlers").get(0).get("id").asLong();
        long selectedBowlerId;

        JsonNode batters = objectMapper.readTree(mockMvc.perform(get("/api/teams/{id}/players", battingTeamId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode bowlers = objectMapper.readTree(mockMvc.perform(get("/api/teams/{id}/players", bowlingTeamId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        selectedBowlerId = bowlers.get(0).get("id").asLong();
        long wicketkeeperId = 0;
        long nextOverBowlerId = 0;
        for (JsonNode player : bowlers) {
            String role = player.get("role").asText();
            long playerId = player.get("id").asLong();
            if ("Wicketkeeper".equals(role)) wicketkeeperId = playerId;
            if (nextOverBowlerId == 0 && playerId != selectedBowlerId
                    && ("Bowler".equals(role) || "All-rounder".equals(role))) nextOverBowlerId = playerId;
        }
        Map<String, Object> delivery = Map.of(
                "strikerId", batters.get(0).get("id").asLong(),
                "nonStrikerId", batters.get(1).get("id").asLong(),
                "bowlerId", selectedBowlerId,
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
                .andExpect(jsonPath("$.currentBowlerId").value(selectedBowlerId))
                .andExpect(jsonPath("$.recentEvents", hasSize(7)));

        Map<String, Object> invalidWide = Map.of(
                "strikerId", batters.get(0).get("id").asLong(),
                "bowlerId", selectedBowlerId,
                "batterRuns", 1,
                "extras", 1,
                "extraType", "WIDE",
                "wicket", false);
        mockMvc.perform(post("/api/matches/{id}/events", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidWide)))
                .andExpect(status().isBadRequest());

        Map<String, Object> wide = Map.of(
                "strikerId", batters.get(0).get("id").asLong(),
                "bowlerId", selectedBowlerId,
                "batterRuns", 0,
                "extras", 2,
                "extraType", "WIDE",
                "wicket", false);
        mockMvc.perform(post("/api/matches/{id}/events", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wide)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runs").value(previousRuns + 6))
                .andExpect(jsonPath("$.extras").value(2))
                .andExpect(jsonPath("$.batters[2].runs").value(4))
                .andExpect(jsonPath("$.recentEvents[0].overLabel").value("1.1 WD"))
                .andExpect(jsonPath("$.recentEvents[1].overLabel").value("1.1"));

        List<MatchEvent> inningsEvents = matchEvents.findByMatchIdAndInningsOrderByIdAsc(matchId, 1);
        MatchEvent misassignedDelivery = inningsEvents.get(inningsEvents.size() - 1);
        misassignedDelivery.reassignBowler(players.findById(wicketkeeperId).orElseThrow());
        matchEvents.save(misassignedDelivery);

        mockMvc.perform(patch("/api/matches/{id}/overs/current/bowler", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("bowlerId", previousOverBowlerId))))
                .andExpect(status().isBadRequest());

        Map<String, Object> changedBowler = Map.of(
                "strikerId", batters.get(0).get("id").asLong(),
                "bowlerId", bowlers.get(1).get("id").asLong(),
                "batterRuns", 0,
                "extras", 0,
                "extraType", "NONE",
                "wicket", false);
        mockMvc.perform(post("/api/matches/{id}/events", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changedBowler)))
                .andExpect(status().isBadRequest());

        Map<String, Object> wicketkeeperAsBowler = Map.of(
                "strikerId", batters.get(0).get("id").asLong(),
                "bowlerId", wicketkeeperId,
                "batterRuns", 0,
                "extras", 0,
                "extraType", "NONE",
                "wicket", false);
        mockMvc.perform(post("/api/matches/{id}/events", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wicketkeeperAsBowler)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/matches/{id}/overs/current/bowler", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("bowlerId", selectedBowlerId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalBalls").value(previousBalls + 1))
                .andExpect(jsonPath("$.currentBowlerId").value(selectedBowlerId));

        Map<String, Object> dotBall = Map.of(
                "strikerId", batters.get(0).get("id").asLong(),
                "bowlerId", selectedBowlerId,
                "batterRuns", 0,
                "extras", 0,
                "extraType", "NONE",
                "wicket", false);
        for (int ball = 0; ball < 5; ball++) {
            mockMvc.perform(post("/api/matches/{id}/events", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dotBall)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/matches/{id}/events", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dotBall)))
                .andExpect(status().isBadRequest());

        Map<String, Object> newOver = Map.of(
                "strikerId", batters.get(0).get("id").asLong(),
                "bowlerId", nextOverBowlerId,
                "batterRuns", 0,
                "extras", 0,
                "extraType", "NONE",
                "wicket", false);
        mockMvc.perform(post("/api/matches/{id}/events", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newOver)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overs").value("2.1"))
                .andExpect(jsonPath("$.currentBowlerId").value(nextOverBowlerId));

        mockMvc.perform(post("/api/matches/{id}/innings/next", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentInnings").value(2))
                .andExpect(jsonPath("$.target").value(previousRuns + 7));

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