package com.cricketscore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "players")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String role;

    private Integer jerseyNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    protected Player() {}

    public Player(String name, String role, Integer jerseyNumber, Team team) {
        this.name = name;
        this.role = role;
        this.jerseyNumber = jerseyNumber;
        this.team = team;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public Integer getJerseyNumber() { return jerseyNumber; }
    public Team getTeam() { return team; }

    public void updateProfile(String name, String role, Integer jerseyNumber) {
        this.name = name;
        this.role = role;
        this.jerseyNumber = jerseyNumber;
    }
}