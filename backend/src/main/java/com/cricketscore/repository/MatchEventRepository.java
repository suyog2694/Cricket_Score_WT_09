package com.cricketscore.repository;

import com.cricketscore.model.MatchEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {
    List<MatchEvent> findByMatchIdAndInningsOrderByIdAsc(Long matchId, Integer innings);
    List<MatchEvent> findByMatchIdAndInningsAndOverNumberOrderByIdAsc(Long matchId, Integer innings, Integer overNumber);
    List<MatchEvent> findByMatchIdOrderByIdDesc(Long matchId);
}