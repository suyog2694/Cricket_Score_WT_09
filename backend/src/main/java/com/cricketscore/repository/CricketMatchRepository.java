package com.cricketscore.repository;

import com.cricketscore.model.CricketMatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CricketMatchRepository extends JpaRepository<CricketMatch, Long> {
    List<CricketMatch> findAllByOrderByStartTimeDesc();
}