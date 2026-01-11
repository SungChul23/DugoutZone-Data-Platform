package com.dev.dugout.domain.team.repository;

import com.dev.dugout.domain.team.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game,Long> {
    // 특정 월의 시작일과 종료일 사이의 모든 경기를 가져옴(feat.JPQL)
    @Query("SELECT g FROM Game g JOIN FETCH g.homeTeam JOIN FETCH g.awayTeam " +
            "WHERE g.gameDate BETWEEN :startDate AND :endDate " +
            "ORDER BY g.gameDate ASC, g.gameTime ASC")
    List<Game> findMonthlySchedule(LocalDate startDate, LocalDate endDate);
}
