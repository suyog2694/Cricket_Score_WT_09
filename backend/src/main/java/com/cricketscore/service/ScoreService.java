package com.cricketscore.service;

import com.cricketscore.api.ApiDtos.BatterLine;
import com.cricketscore.api.ApiDtos.BowlerLine;
import com.cricketscore.api.ApiDtos.EventLine;
import com.cricketscore.api.ApiDtos.InningsLine;
import com.cricketscore.api.ApiDtos.MatchView;
import com.cricketscore.api.ApiDtos.TeamView;
import com.cricketscore.model.CricketMatch;
import com.cricketscore.model.ExtraType;
import com.cricketscore.model.MatchEvent;
import com.cricketscore.model.Player;
import com.cricketscore.model.Team;
import com.cricketscore.repository.CricketMatchRepository;
import com.cricketscore.repository.MatchEventRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ScoreService {
    private final CricketMatchRepository matches;
    private final MatchEventRepository events;

    public ScoreService(CricketMatchRepository matches, MatchEventRepository events) {
        this.matches = matches;
        this.events = events;
    }

    public MatchView getScore(Long id) {
        CricketMatch match = matches.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
        List<MatchEvent> currentEvents = events.findByMatchIdAndInningsOrderByIdAsc(id, match.getCurrentInnings());
        int runs = currentEvents.stream().mapToInt(event -> event.getBatterRuns() + event.getExtras()).sum();
        int wickets = (int) currentEvents.stream().filter(event -> Boolean.TRUE.equals(event.getWicket())).count();
        int legalBalls = (int) currentEvents.stream().filter(ScoreService::isLegal).count();
        List<BatterLine> batters = batterLines(currentEvents);
        List<BowlerLine> bowlers = bowlerLines(currentEvents);
        List<InningsLine> innings = inningsLines(id);
        Integer target = match.getCurrentInnings() == 2 && !innings.isEmpty()
                ? innings.get(0).runs() + 1 : null;
        int oversLeft = match.getOversLimit() * 6 - legalBalls;
        double requiredRate = target == null || oversLeft <= 0 ? 0
                : round((target - runs) * 6.0 / oversLeft);
        List<EventLine> recent = events.findByMatchIdOrderByIdDesc(id).stream().limit(12).map(this::eventLine).toList();
        return new MatchView(match.getId(), match.getTitle(), match.getVenue(), match.getStartTime(),
                match.getStatus(), match.getOversLimit(), match.getCurrentInnings(), teamView(match.getTeamA()),
                teamView(match.getTeamB()), teamView(match.battingTeam()), teamView(match.bowlingTeam()),
                runs, wickets, legalBalls, overLabel(legalBalls), legalBalls == 0 ? 0 : round(runs * 6.0 / legalBalls),
                target, requiredRate, batters, bowlers, recent, innings);
    }

    private List<BatterLine> batterLines(List<MatchEvent> list) {
        Map<Long, BatterAccumulator> stats = new LinkedHashMap<>();
        Long strikerId = list.isEmpty() ? null : list.get(list.size() - 1).getStriker().getId();
        for (MatchEvent event : list) {
            BatterAccumulator line = stats.computeIfAbsent(event.getStriker().getId(), ignored -> new BatterAccumulator(event.getStriker()));
            line.runs += event.getBatterRuns();
            if (event.getExtraType() != ExtraType.WIDE) line.balls++;
            if (event.getBatterRuns() == 4) line.fours++;
            if (event.getBatterRuns() == 6) line.sixes++;
        }
        return stats.values().stream().map(line -> new BatterLine(line.player.getId(), line.player.getName(),
                line.runs, line.balls, line.fours, line.sixes,
                line.balls == 0 ? 0 : round(line.runs * 100.0 / line.balls),
                line.player.getId().equals(strikerId))).toList();
    }

    private List<BowlerLine> bowlerLines(List<MatchEvent> list) {
        Map<Long, BowlerAccumulator> stats = new LinkedHashMap<>();
        for (MatchEvent event : list) {
            BowlerAccumulator line = stats.computeIfAbsent(event.getBowler().getId(), ignored -> new BowlerAccumulator(event.getBowler()));
            if (isLegal(event)) line.balls++;
            if (event.getExtraType() != ExtraType.BYE && event.getExtraType() != ExtraType.LEG_BYE
                    && event.getExtraType() != ExtraType.PENALTY) line.runs += event.getBatterRuns() + event.getExtras();
            if (Boolean.TRUE.equals(event.getWicket()) && event.getDismissalType() != null
                    && !event.getDismissalType().equalsIgnoreCase("run out")) line.wickets++;
        }
        return stats.values().stream().map(line -> new BowlerLine(line.player.getId(), line.player.getName(),
                overLabel(line.balls), line.runs, line.wickets,
                line.balls == 0 ? 0 : round(line.runs * 6.0 / line.balls))).toList();
    }

    private List<InningsLine> inningsLines(Long matchId) {
        List<MatchEvent> all = events.findByMatchIdOrderByIdDesc(matchId);
        Map<Integer, List<MatchEvent>> byInnings = new LinkedHashMap<>();
        all.stream().sorted(Comparator.comparing(MatchEvent::getId)).forEach(event ->
                byInnings.computeIfAbsent(event.getInnings(), ignored -> new ArrayList<>()).add(event));
        return byInnings.entrySet().stream().map(entry -> {
            CricketMatch match = entry.getValue().get(0).getMatch();
            Team battingTeam = entry.getKey() % 2 == 1 ? match.getBattingFirstTeam()
                : (match.getBattingFirstTeam().getId().equals(match.getTeamA().getId()) ? match.getTeamB() : match.getTeamA());
            int runs = entry.getValue().stream().mapToInt(event -> event.getBatterRuns() + event.getExtras()).sum();
            int wickets = (int) entry.getValue().stream().filter(event -> Boolean.TRUE.equals(event.getWicket())).count();
            int balls = (int) entry.getValue().stream().filter(ScoreService::isLegal).count();
            return new InningsLine(entry.getKey(), battingTeam.getName(), runs, wickets, overLabel(balls));
        }).toList();
    }

    private EventLine eventLine(MatchEvent event) {
        String description;
        if (Boolean.TRUE.equals(event.getWicket())) description = "WICKET · " + event.getDismissalType();
        else if (event.getExtraType() != ExtraType.NONE) description = event.getExtraType().name().replace('_', ' ') + " · " + (event.getBatterRuns() + event.getExtras());
        else description = event.getBatterRuns() == 0 ? "Dot ball" : event.getBatterRuns() + " run" + (event.getBatterRuns() == 1 ? "" : "s");
        return new EventLine(event.getId(), event.getOverNumber() + "." + event.getBallNumber(), description,
                event.getBatterRuns() + event.getExtras(), Boolean.TRUE.equals(event.getWicket()),
                event.getExtraType(), event.getCreatedAt());
    }

    private TeamView teamView(Team team) { return new TeamView(team.getId(), team.getName(), team.getShortName()); }
    private static boolean isLegal(MatchEvent event) { return event.getExtraType() != ExtraType.WIDE && event.getExtraType() != ExtraType.NO_BALL; }
    private static String overLabel(int balls) { return (balls / 6) + "." + (balls % 6); }
    private static double round(double value) { return Math.round(value * 100.0) / 100.0; }

    private static class BatterAccumulator {
        private final Player player;
        private int runs, balls, fours, sixes;
        private BatterAccumulator(Player player) { this.player = player; }
    }

    private static class BowlerAccumulator {
        private final Player player;
        private int balls, runs, wickets;
        private BowlerAccumulator(Player player) { this.player = player; }
    }
}