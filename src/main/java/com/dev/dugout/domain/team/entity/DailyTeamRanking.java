package com.dev.dugout.domain.team.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@IdClass(TeamRankingId.class) //복합키 생성

//추후 월요일마다 각 팀의 상성 분석 후 최신화된 팀 예측 인사이트 제공
public class DailyTeamRanking {
    @Id
    private LocalDate rankingDate;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    private Integer rank;
    private Double winRate;
    private String recent10Games;

    // AWS DEA 고도화 예측 데이터
    private Integer aiPredictedRank;

}
