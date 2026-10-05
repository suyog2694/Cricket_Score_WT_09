package com.cricketscore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_events")
public class MatchEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private CricketMatch match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "striker_id", nullable = false)
    private Player striker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "non_striker_id")
    private Player nonStriker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bowler_id", nullable = false)
    private Player bowler;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wicket_player_id")
    private Player wicketPlayer;

    private Integer innings;
    private Integer overNumber;
    private Integer ballNumber;
    private Integer batterRuns;
    private Integer extras;
    private Boolean wicket;
    private String dismissalType;
    private String note;
    private LocalDateTime createdAt;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private ExtraType extraType;

    protected MatchEvent() {}

    public MatchEvent(CricketMatch match, Player striker, Player nonStriker, Player bowler,
                      Player wicketPlayer, int innings, int overNumber, int ballNumber,
                      int batterRuns, int extras, ExtraType extraType, boolean wicket,
                      String dismissalType, String note) {
        this.match = match;
        this.striker = striker;
        this.nonStriker = nonStriker;
        this.bowler = bowler;
        this.wicketPlayer = wicketPlayer;
        this.innings = innings;
        this.overNumber = overNumber;
        this.ballNumber = ballNumber;
        this.batterRuns = batterRuns;
        this.extras = extras;
        this.extraType = extraType;
        this.wicket = wicket;
        this.dismissalType = dismissalType;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public CricketMatch getMatch() { return match; }
    public Player getStriker() { return striker; }
    public Player getNonStriker() { return nonStriker; }
    public Player getBowler() { return bowler; }
    public Player getWicketPlayer() { return wicketPlayer; }
    public Integer getInnings() { return innings; }
    public Integer getOverNumber() { return overNumber; }
    public Integer getBallNumber() { return ballNumber; }
    public Integer getBatterRuns() { return batterRuns; }
    public Integer getExtras() { return extras; }
    public ExtraType getExtraType() { return extraType; }
    public Boolean getWicket() { return wicket; }
    public String getDismissalType() { return dismissalType; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void reassignBowler(Player bowler) {
        this.bowler = bowler;
    }
}