package com.cricketscore.config;

import com.cricketscore.model.CricketMatch;
import com.cricketscore.model.ExtraType;
import com.cricketscore.model.MatchEvent;
import com.cricketscore.model.Player;
import com.cricketscore.model.Team;
import com.cricketscore.repository.CricketMatchRepository;
import com.cricketscore.repository.MatchEventRepository;
import com.cricketscore.repository.PlayerRepository;
import com.cricketscore.repository.TeamRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class SampleDataLoader {
        private static final List<PlayerSeed> INDIA = List.of(
                        new PlayerSeed("Rohit Sharma", "Batter", 45),
                        new PlayerSeed("Virat Kohli", "Batter", 18),
                        new PlayerSeed("Shubman Gill", "Batter", 77),
                        new PlayerSeed("KL Rahul", "Wicketkeeper", 1),
                        new PlayerSeed("Hardik Pandya", "All-rounder", 33),
                        new PlayerSeed("Ravindra Jadeja", "All-rounder", 8),
                        new PlayerSeed("Axar Patel", "All-rounder", 20),
                        new PlayerSeed("Jasprit Bumrah", "Bowler", 93),
                        new PlayerSeed("Mohammed Siraj", "Bowler", 73),
                        new PlayerSeed("Kuldeep Yadav", "Bowler", 23),
                        new PlayerSeed("Arshdeep Singh", "Bowler", 2));

        private static final List<PlayerSeed> ENGLAND = List.of(
                        new PlayerSeed("Jos Buttler", "Wicketkeeper", 63),
                        new PlayerSeed("Phil Salt", "Batter", 61),
                        new PlayerSeed("Joe Root", "Batter", 66),
                        new PlayerSeed("Jofra Archer", "Bowler", 22),
                        new PlayerSeed("Harry Brook", "Batter", 88),
                        new PlayerSeed("Ben Stokes", "All-rounder", 55),
                        new PlayerSeed("Liam Livingstone", "All-rounder", 23),
                        new PlayerSeed("Sam Curran", "All-rounder", 58),
                        new PlayerSeed("Mark Wood", "Bowler", 33),
                        new PlayerSeed("Adil Rashid", "Bowler", 95),
                        new PlayerSeed("Reece Topley", "Bowler", 6));

        private static final List<PlayerSeed> AUSTRALIA = List.of(
                        new PlayerSeed("Travis Head", "Batter", 62),
                        new PlayerSeed("David Warner", "Batter", 31),
                        new PlayerSeed("Steve Smith", "Batter", 49),
                        new PlayerSeed("Marnus Labuschagne", "Batter", 33),
                        new PlayerSeed("Glenn Maxwell", "All-rounder", 32),
                        new PlayerSeed("Mitchell Marsh", "All-rounder", 8),
                        new PlayerSeed("Alex Carey", "Wicketkeeper", 4),
                        new PlayerSeed("Pat Cummins", "Bowler", 30),
                        new PlayerSeed("Mitchell Starc", "Bowler", 56),
                        new PlayerSeed("Josh Hazlewood", "Bowler", 38),
                        new PlayerSeed("Adam Zampa", "Bowler", 4));

    @Bean
    CommandLineRunner loadSampleData(TeamRepository teams, PlayerRepository players,
                                     CricketMatchRepository matches, MatchEventRepository events) {
                return args -> seedOrUpgrade(teams, players, matches, events);
        }

        @Transactional
        void seedOrUpgrade(TeamRepository teams, PlayerRepository players,
                                           CricketMatchRepository matches, MatchEventRepository events) {
                if (teams.count() == 0) {
                        Team india = teams.save(new Team("India", "IND"));
                        Team england = teams.save(new Team("England", "ENG"));
                        Team australia = teams.save(new Team("Australia", "AUS"));
                        List<Player> indiaPlayers = createRoster(india, INDIA, players);
                        List<Player> englandPlayers = createRoster(england, ENGLAND, players);
                        createRoster(australia, AUSTRALIA, players);
                        CricketMatch match = matches.save(new CricketMatch("India vs England · 2nd ODI",
                                        "Narendra Modi Stadium · Ahmedabad", 50, LocalDateTime.now(), india, england, india));
                        addOpeningOver(match, indiaPlayers, englandPlayers, events);
                        return;
                }

                Optional<Team> legacyIndia = teams.findAll().stream().filter(team -> "MUF".equals(team.getShortName())).findFirst();
                Optional<Team> legacyEngland = teams.findAll().stream().filter(team -> "CHS".equals(team.getShortName())).findFirst();
                if (legacyIndia.isEmpty() || legacyEngland.isEmpty()) return;

                Team india = legacyIndia.get();
                Team england = legacyEngland.get();
                india.updateIdentity("India", "IND");
                england.updateIdentity("England", "ENG");
                teams.saveAll(List.of(india, england));
                upgradeRoster(india, INDIA, players);
                upgradeRoster(england, ENGLAND, players);

                Team australia = teams.findAll().stream().filter(team -> "AUS".equals(team.getShortName())).findFirst()
                                .orElseGet(() -> teams.save(new Team("Australia", "AUS")));
                upgradeRoster(australia, AUSTRALIA, players);

                List<CricketMatch> upgradedMatches = new ArrayList<>();
                for (CricketMatch match : matches.findAll()) {
                        if (match.getTeamA().getId().equals(india.getId()) && match.getTeamB().getId().equals(england.getId())) {
                                match.updateFixtureDetails("India vs England · 2nd ODI", "Narendra Modi Stadium · Ahmedabad");
                                upgradedMatches.add(match);
                        }
                }
                matches.saveAll(upgradedMatches);
                matches.flush();
        }

        private List<Player> createRoster(Team team, List<PlayerSeed> roster, PlayerRepository players) {
                return players.saveAll(roster.stream()
                                .map(seed -> new Player(seed.name(), seed.role(), seed.jerseyNumber(), team)).toList());
        }

        private void upgradeRoster(Team team, List<PlayerSeed> roster, PlayerRepository players) {
                List<Player> current = new ArrayList<>(players.findByTeamIdOrderByIdAsc(team.getId()));
                int existingCount = Math.min(current.size(), roster.size());
                for (int index = 0; index < existingCount; index++) {
                        PlayerSeed seed = roster.get(index);
                        current.get(index).updateProfile(seed.name(), seed.role(), seed.jerseyNumber());
                }
                players.saveAll(current.subList(0, existingCount));
                if (current.size() < roster.size()) {
                        createRoster(team, roster.subList(current.size(), roster.size()), players);
                }
        }

        private void addOpeningOver(CricketMatch match, List<Player> batters, List<Player> bowlers,
                                                                MatchEventRepository events) {
                int[] runs = {1, 0, 4, 1, 2, 0};
                for (int ball = 0; ball < runs.length; ball++) {
                        Player striker = batters.get(ball % 2);
                        Player nonStriker = batters.get((ball + 1) % 2);
                        events.save(new MatchEvent(match, striker, nonStriker, bowlers.get(3), null,
                                        1, 0, ball + 1, runs[ball], 0, ExtraType.NONE, false, null, null));
                }
    }

        private record PlayerSeed(String name, String role, int jerseyNumber) {}
}