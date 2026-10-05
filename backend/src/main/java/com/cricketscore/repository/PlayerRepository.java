package com.cricketscore.repository;

import com.cricketscore.model.Player;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByTeamIdOrderByName(Long teamId);
    List<Player> findByTeamIdOrderByIdAsc(Long teamId);
}