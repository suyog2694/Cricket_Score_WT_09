package com.cricketscore.api;

import com.cricketscore.api.ApiDtos.DeliveryRequest;
import com.cricketscore.api.ApiDtos.BowlerCorrectionRequest;
import com.cricketscore.api.ApiDtos.MatchRequest;
import com.cricketscore.api.ApiDtos.MatchView;
import com.cricketscore.model.CricketMatch;
import com.cricketscore.model.MatchEvent;
import com.cricketscore.service.DeliveryService;
import com.cricketscore.service.MatchService;
import com.cricketscore.service.ScoreService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchController {
    private final MatchService matches;
    private final ScoreService scores;
    private final DeliveryService deliveries;

    public MatchController(MatchService matches, ScoreService scores, DeliveryService deliveries) {
        this.matches = matches;
        this.scores = scores;
        this.deliveries = deliveries;
    }

    @GetMapping
    public List<MatchView> matches() { return matches.listMatches().stream().map(match -> scores.getScore(match.getId())).toList(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchView create(@RequestBody MatchRequest request) {
        CricketMatch match = matches.createMatch(request);
        return scores.getScore(match.getId());
    }

    @GetMapping("/{id}")
    public MatchView score(@PathVariable Long id) { return scores.getScore(id); }

    @PostMapping("/{id}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchView record(@PathVariable Long id, @RequestBody DeliveryRequest request) {
        MatchEvent ignored = deliveries.record(id, request);
        return scores.getScore(id);
    }

    @PatchMapping("/{id}/overs/current/bowler")
    public MatchView correctCurrentOverBowler(@PathVariable Long id, @RequestBody BowlerCorrectionRequest request) {
        deliveries.reassignCurrentOver(id, request.bowlerId());
        return scores.getScore(id);
    }

    @PostMapping("/{id}/innings/next")
    public MatchView nextInnings(@PathVariable Long id) {
        matches.nextInnings(id);
        return scores.getScore(id);
    }

    @PostMapping("/{id}/finish")
    public MatchView finish(@PathVariable Long id) {
        matches.finish(id);
        return scores.getScore(id);
    }
}