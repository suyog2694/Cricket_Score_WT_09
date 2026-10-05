package com.cricketscore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
public class CricketMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String venue;

    @Min(1)
    private Integer oversLimit;

    private LocalDateTime startTime;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private Integer currentInnings;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_a_id", nullable = false)
    private Team teamA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_b_id", nullable = false)
    private Team teamB;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batting_first_team_id", nullable = false)
    private Team battingFirstTeam;

    protected CricketMatch() {}

    public CricketMatch(String title, String venue, Integer oversLimit, LocalDateTime startTime,
                        Team teamA, Team teamB, Team battingFirstTeam) {
        this.title = title;
        this.venue = venue;
        this.oversLimit = oversLimit;
        this.startTime = startTime;
        this.teamA = teamA;
        this.teamB = teamB;
        this.battingFirstTeam = battingFirstTeam;
        this.status = MatchStatus.LIVE;
        this.currentInnings = 1;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getVenue() { return venue; }
    public Integer getOversLimit() { return oversLimit; }
    public LocalDateTime getStartTime() { return startTime; }
    public MatchStatus getStatus() { return status; }
    public Integer getCurrentInnings() { return currentInnings; }
    public Team getTeamA() { return teamA; }
    public Team getTeamB() { return teamB; }
    public Team getBattingFirstTeam() { return battingFirstTeam; }

    public Team battingTeam() {
        return currentInnings % 2 == 1 ? battingFirstTeam : opposing(battingFirstTeam);
    }

    public Team bowlingTeam() { return opposing(battingTeam()); }

    private Team opposing(Team team) { return team.getId().equals(teamA.getId()) ? teamB : teamA; }

    public void nextInnings() {
        if (currentInnings >= 2) {
            throw new IllegalStateException("Both innings have already started");
        } else {
            currentInnings++;
        }
    }

    public void finish() {
        status = MatchStatus.COMPLETED;
    }
}