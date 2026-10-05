package com.cricketscore.service;

import com.cricketscore.api.ApiDtos.PlayerRequest;
import com.cricketscore.api.ApiDtos.PlayerView;
import com.cricketscore.api.ApiDtos.TeamRequest;
import com.cricketscore.api.ApiDtos.TeamView;
import com.cricketscore.model.Player;
import com.cricketscore.model.Team;
import com.cricketscore.repository.PlayerRepository;
import com.cricketscore.repository.TeamRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TeamService {
    private final TeamRepository teams;
    private final PlayerRepository players;

    public TeamService(TeamRepository teams, PlayerRepository players) {
        this.teams = teams;
        this.players = players;
    }

    public List<TeamView> listTeams() {
        return teams.findAll().stream().map(this::view).toList();
    }

    public TeamView createTeam(TeamRequest request) {
        if (request.name() == null || request.name().isBlank() || request.shortName() == null || request.shortName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team name and short name are required");
        }
        return view(teams.save(new Team(request.name().trim(), request.shortName().trim().toUpperCase())));
    }

    public List<PlayerView> listPlayers(Long teamId) {
        if (!teams.existsById(teamId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found");
        return players.findByTeamIdOrderByName(teamId).stream().map(this::view).toList();
    }

    public PlayerView createPlayer(Long teamId, PlayerRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player name is required");
        }
        Team team = teams.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
        String role = request.role() == null || request.role().isBlank() ? "All-rounder" : request.role();
        return view(players.save(new Player(request.name().trim(), role, request.jerseyNumber(), team)));
    }

    private TeamView view(Team team) {
        return new TeamView(team.getId(), team.getName(), team.getShortName());
    }

    private PlayerView view(Player player) {
        return new PlayerView(player.getId(), player.getName(), player.getRole(), player.getJerseyNumber(),
                player.getTeam().getId(), player.getTeam().getName());
    }
}