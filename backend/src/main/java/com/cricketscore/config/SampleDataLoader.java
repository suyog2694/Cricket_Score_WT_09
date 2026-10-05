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
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleDataLoader {
    @Bean
    CommandLineRunner loadSampleData(TeamRepository teams, PlayerRepository players,
                                     CricketMatchRepository matches, MatchEventRepository events) {
        return args -> {
            if (teams.count() > 0) return;
            Team falcons = teams.save(new Team("Mumbai Falcons", "MUF"));
            Team strikers = teams.save(new Team("Chennai Strikers", "CHS"));
            List<Player> falconPlayers = players.saveAll(List.of(
                    new Player("Aarav Mehta", "Batter", 18, falcons),
                    new Player("Rohan Kulkarni", "Batter", 7, falcons),
                    new Player("Dev Shah", "All-rounder", 32, falcons),
                    new Player("Kabir Joshi", "Bowler", 11, falcons),
                    new Player("Ishaan Rao", "Bowler", 24, falcons)));
            List<Player> strikerPlayers = players.saveAll(List.of(
                    new Player("Arjun Nair", "Batter", 9, strikers),
                    new Player("Vikram Iyer", "Batter", 21, strikers),
                    new Player("Siddharth Menon", "All-rounder", 8, strikers),
                    new Player("Karthik Das", "Bowler", 14, strikers),
                    new Player("Neil Thomas", "Bowler", 3, strikers)));
            CricketMatch match = matches.save(new CricketMatch("MUF vs CHS · Match 18", "Wankhede Stadium, Mumbai",
                    20, LocalDateTime.now(), falcons, strikers, falcons));
            int[][] deliveries = {{1, 0}, {0, 0}, {4, 0}, {1, 0}, {2, 0}, {0, 0}, {0, 0}, {6, 0}, {1, 0}, {0, 0}, {1, 1}};
            for (int i = 0; i < deliveries.length; i++) {
                int legal = i;
                int runs = deliveries[i][0];
                int extras = deliveries[i][1];
                ExtraType type = extras > 0 ? ExtraType.WIDE : ExtraType.NONE;
                events.save(new MatchEvent(match, falconPlayers.get(i == 5 ? 1 : 0), falconPlayers.get(1),
                        strikerPlayers.get(3), null, 1, legal / 6, legal % 6 + 1,
                        runs, extras, type, false, null, null));
            }
        };
    }
}