package com.cricketscore.repository;

import com.cricketscore.model.MatchEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {
    List<MatchEvent> findByMatchIdAndInningsOrderByIdAsc(Long matchId, Integer innings);
    List<MatchEvent> findByMatchIdOrderByIdDesc(Long matchId);
}