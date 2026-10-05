package com.cricketscore.service;

import com.cricketscore.api.ApiDtos.MatchRequest;
import com.cricketscore.model.CricketMatch;
import com.cricketscore.model.Team;
import com.cricketscore.repository.CricketMatchRepository;
import com.cricketscore.repository.TeamRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class MatchService {
    private final CricketMatchRepository matches;
    private final TeamRepository teams;

    public MatchService(CricketMatchRepository matches, TeamRepository teams) {
        this.matches = matches;
        this.teams = teams;
    }

    @Transactional(readOnly = true)
    public List<CricketMatch> listMatches() {
        return matches.findAllByOrderByStartTimeDesc();
    }

    @Transactional(readOnly = true)
    public CricketMatch getMatch(Long id) {
        return matches.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
    }

    public CricketMatch createMatch(MatchRequest request) {
        if (request.teamAId() == null || request.teamBId() == null || request.oversLimit() == null
                || request.oversLimit() < 1 || request.teamAId().equals(request.teamBId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose two different teams and a positive over limit");
        }
        Team teamA = team(request.teamAId());
        Team teamB = team(request.teamBId());
        Long battingId = request.battingFirstTeamId() == null ? teamA.getId() : request.battingFirstTeamId();
        if (!battingId.equals(teamA.getId()) && !battingId.equals(teamB.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batting first team must be part of the match");
        }
        Team battingFirst = battingId.equals(teamA.getId()) ? teamA : teamB;
        String title = request.title() == null || request.title().isBlank()
                ? teamA.getShortName() + " vs " + teamB.getShortName() : request.title().trim();
        return matches.save(new CricketMatch(title, request.venue(), request.oversLimit(), LocalDateTime.now(),
                teamA, teamB, battingFirst));
    }

    public CricketMatch nextInnings(Long id) {
        CricketMatch match = getMatch(id);
        if (match.getStatus().name().equals("COMPLETED")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match is already completed");
        }
        if (match.getCurrentInnings() >= 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The second innings has already started");
        }
        match.nextInnings();
        return matches.save(match);
    }

    public CricketMatch finish(Long id) {
        CricketMatch match = getMatch(id);
        if (match.getStatus().name().equals("COMPLETED")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match is already completed");
        }
        match.finish();
        return matches.save(match);
    }

    private Team team(Long id) {
        return teams.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
    }
}