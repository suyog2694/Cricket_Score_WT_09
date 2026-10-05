package com.cricketscore.api;

import com.cricketscore.model.ExtraType;
import com.cricketscore.model.MatchStatus;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}

    public record TeamView(Long id, String name, String shortName) {}
    public record PlayerView(Long id, String name, String role, Integer jerseyNumber, Long teamId, String teamName) {}
    public record TeamRequest(String name, String shortName) {}
    public record PlayerRequest(String name, String role, Integer jerseyNumber) {}
    public record MatchRequest(String title, String venue, Integer oversLimit, Long teamAId,
                               Long teamBId, Long battingFirstTeamId) {}
    public record DeliveryRequest(Long strikerId, Long nonStrikerId, Long bowlerId,
                                  Integer batterRuns, Integer extras, ExtraType extraType,
                                  Boolean wicket, Long wicketPlayerId, String dismissalType, String note) {}
    public record BatterLine(Long id, String name, int runs, int balls, int fours, int sixes,
                             double strikeRate, boolean onStrike) {}
    public record BowlerLine(Long id, String name, String overs, int runs, int wickets, double economy) {}
    public record EventLine(Long id, String overLabel, String description, int runs,
                           boolean wicket, ExtraType extraType, LocalDateTime createdAt) {}
    public record InningsLine(int number, String team, int runs, int wickets, String overs) {}
    public record MatchView(Long id, String title, String venue, LocalDateTime startTime,
                            MatchStatus status, int oversLimit, int currentInnings,
                            TeamView teamA, TeamView teamB, TeamView battingTeam, TeamView bowlingTeam,
                            int runs, int wickets, int legalBalls, String overs, double runRate,
                            Integer target, double requiredRunRate, List<BatterLine> batters,
                            List<BowlerLine> bowlers, List<EventLine> recentEvents,
                            List<InningsLine> innings) {}
}