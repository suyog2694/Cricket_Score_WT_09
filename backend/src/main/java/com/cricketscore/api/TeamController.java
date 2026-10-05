package com.cricketscore.api;

import com.cricketscore.api.ApiDtos.PlayerRequest;
import com.cricketscore.api.ApiDtos.PlayerView;
import com.cricketscore.api.ApiDtos.TeamRequest;
import com.cricketscore.api.ApiDtos.TeamView;
import com.cricketscore.service.TeamService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TeamController {
    private final TeamService service;
    public TeamController(TeamService service) { this.service = service; }

    @GetMapping("/teams")
    public List<TeamView> teams() { return service.listTeams(); }

    @PostMapping("/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamView createTeam(@RequestBody TeamRequest request) { return service.createTeam(request); }

    @GetMapping("/teams/{teamId}/players")
    public List<PlayerView> players(@PathVariable Long teamId) { return service.listPlayers(teamId); }

    @PostMapping("/teams/{teamId}/players")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerView createPlayer(@PathVariable Long teamId, @RequestBody PlayerRequest request) {
        return service.createPlayer(teamId, request);
    }
}