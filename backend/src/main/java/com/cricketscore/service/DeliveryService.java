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
        if (extraType != ExtraType.NONE && extraType != ExtraType.NO_BALL && batterRuns > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wides, byes, leg-byes, and penalties cannot be batter runs");
        }
        if ((extraType == ExtraType.WIDE || extraType == ExtraType.NO_BALL) && extras < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wides and no-balls must include at least one extra run");
        }
        Player striker = player(request.strikerId());
        Player nonStriker = request.nonStrikerId() == null ? null : player(request.nonStrikerId());
        Player bowler = player(request.bowlerId());
        Player dismissed = request.wicketPlayerId() == null ? null : player(request.wicketPlayerId());
        if (!striker.getTeam().getId().equals(match.battingTeam().getId())
                || (nonStriker != null && !nonStriker.getTeam().getId().equals(match.battingTeam().getId()))
                || !bowler.getTeam().getId().equals(match.bowlingTeam().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Players must belong to the active batting and bowling teams");
        }
        if (!"Bowler".equalsIgnoreCase(bowler.getRole())
                && !"All-rounder".equalsIgnoreCase(bowler.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only bowlers and all-rounders can bowl");
        }
        boolean wicket = Boolean.TRUE.equals(request.wicket());
        if (wicket && dismissed == null) dismissed = striker;
        if (dismissed != null && !dismissed.getTeam().getId().equals(match.battingTeam().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dismissed player must belong to the batting team");
        }
        List<MatchEvent> inningsEvents = events.findByMatchIdAndInningsOrderByIdAsc(matchId, match.getCurrentInnings());
        int legalBalls = (int) inningsEvents.stream().filter(event -> event.getExtraType() != ExtraType.WIDE
                && event.getExtraType() != ExtraType.NO_BALL).count();
        int currentOver = legalBalls / 6;
        MatchEvent currentOverLastEvent = inningsEvents.stream()
            .filter(event -> event.getOverNumber() == currentOver)
            .reduce((first, second) -> second).orElse(null);
        if (currentOverLastEvent != null
            && !currentOverLastEvent.getBowler().getId().equals(bowler.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The same bowler must complete the current over");
        }
        if (currentOverLastEvent == null && legalBalls > 0 && legalBalls % 6 == 0) {
            MatchEvent previousOverLastLegal = inningsEvents.stream()
                .filter(event -> event.getOverNumber() == currentOver - 1
                    && event.getExtraType() != ExtraType.WIDE && event.getExtraType() != ExtraType.NO_BALL)
                .reduce((first, second) -> second).orElse(null);
            if (previousOverLastLegal != null
                && previousOverLastLegal.getBowler().getId().equals(bowler.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A bowler cannot bowl consecutive overs");
            }
        }
        if (legalBalls >= match.getOversLimit() * 6 && extraType != ExtraType.WIDE && extraType != ExtraType.NO_BALL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The innings has reached its over limit");
        }
        return events.save(new MatchEvent(match, striker, nonStriker, bowler, dismissed,
                match.getCurrentInnings(), legalBalls / 6, legalBalls % 6 + 1,
                batterRuns, extras, extraType, wicket, request.dismissalType(), request.note()));
    }

    public void reassignCurrentOver(Long matchId, Long bowlerId) {
        CricketMatch match = matches.getMatch(matchId);
        if (match.getStatus() != MatchStatus.LIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match is not live");
        }
        Player bowler = player(bowlerId);
        if (!bowler.getTeam().getId().equals(match.bowlingTeam().getId())
                || (!"Bowler".equalsIgnoreCase(bowler.getRole())
                && !"All-rounder".equalsIgnoreCase(bowler.getRole()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose a bowler or all-rounder from the bowling team");
        }
        List<MatchEvent> inningsEvents = events.findByMatchIdAndInningsOrderByIdAsc(matchId, match.getCurrentInnings());
        int legalBalls = (int) inningsEvents.stream().filter(DeliveryService::isLegal).count();
        int currentOver = legalBalls / 6;
        List<MatchEvent> overEvents = events.findByMatchIdAndInningsAndOverNumberOrderByIdAsc(
                matchId, match.getCurrentInnings(), currentOver);
        if (overEvents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The current over has no deliveries to correct");
        }
        Long previousBowlerId = inningsEvents.stream()
            .filter(event -> event.getOverNumber() == currentOver - 1 && isLegal(event))
            .reduce((first, second) -> second).map(event -> event.getBowler().getId()).orElse(null);
        if (bowler.getId().equals(previousBowlerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A bowler cannot bowl consecutive overs");
        }
        overEvents.forEach(event -> event.reassignBowler(bowler));
        events.saveAll(overEvents);
    }

    private Player player(Long id) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required player selection is missing");
        return players.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
    }

    private static boolean isLegal(MatchEvent event) {
        return event.getExtraType() != ExtraType.WIDE && event.getExtraType() != ExtraType.NO_BALL;
    }
}