package com.cricketscore.service;

import com.cricketscore.api.ApiDtos.DeliveryRequest;
import com.cricketscore.model.CricketMatch;
import com.cricketscore.model.ExtraType;
import com.cricketscore.model.MatchEvent;
import com.cricketscore.model.MatchStatus;
import com.cricketscore.model.Player;
import com.cricketscore.repository.MatchEventRepository;
import com.cricketscore.repository.PlayerRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class DeliveryService {
    private final MatchService matches;
    private final MatchEventRepository events;
    private final PlayerRepository players;

    public DeliveryService(MatchService matches, MatchEventRepository events, PlayerRepository players) {
        this.matches = matches;
        this.events = events;
        this.players = players;
    }

    public MatchEvent record(Long matchId, DeliveryRequest request) {
        CricketMatch match = matches.getMatch(matchId);
        if (match.getStatus() != MatchStatus.LIVE) throw new ResponseStatusException(HttpStatus.CONFLICT, "Match is not live");
        ExtraType extraType = request.extraType() == null ? ExtraType.NONE : request.extraType();
        int batterRuns = request.batterRuns() == null ? 0 : request.batterRuns();
        int extras = request.extras() == null ? 0 : request.extras();
        if (batterRuns < 0 || extras < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Runs cannot be negative");
        Player striker = player(request.strikerId());
        Player nonStriker = request.nonStrikerId() == null ? null : player(request.nonStrikerId());
        Player bowler = player(request.bowlerId());
        Player dismissed = request.wicketPlayerId() == null ? null : player(request.wicketPlayerId());
        if (!striker.getTeam().getId().equals(match.battingTeam().getId())
                || (nonStriker != null && !nonStriker.getTeam().getId().equals(match.battingTeam().getId()))
                || !bowler.getTeam().getId().equals(match.bowlingTeam().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Players must belong to the active batting and bowling teams");
        }
        boolean wicket = Boolean.TRUE.equals(request.wicket());
        if (wicket && dismissed == null) dismissed = striker;
        if (dismissed != null && !dismissed.getTeam().getId().equals(match.battingTeam().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dismissed player must belong to the batting team");
        }
        List<MatchEvent> inningsEvents = events.findByMatchIdAndInningsOrderByIdAsc(matchId, match.getCurrentInnings());
        int legalBalls = (int) inningsEvents.stream().filter(event -> event.getExtraType() != ExtraType.WIDE
                && event.getExtraType() != ExtraType.NO_BALL).count();
        if (legalBalls >= match.getOversLimit() * 6 && extraType != ExtraType.WIDE && extraType != ExtraType.NO_BALL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The innings has reached its over limit");
        }
        return events.save(new MatchEvent(match, striker, nonStriker, bowler, dismissed,
                match.getCurrentInnings(), legalBalls / 6, legalBalls % 6 + 1,
                batterRuns, extras, extraType, wicket, request.dismissalType(), request.note()));
    }

    private Player player(Long id) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required player selection is missing");
        return players.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
    }
}