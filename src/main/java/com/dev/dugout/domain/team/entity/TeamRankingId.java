package com.dev.dugout.domain.team.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

//DailyTeamRanking 엔티티의 복합키를 정의하는 클래스
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // 필수: JPA가 식별자를 비교할 때 사용합니다.
public class TeamRankingId implements Serializable {

    private LocalDate rankingDate;
    private Long team; // @ManyToOne인 경우 연관된 엔티티의 ID 타입

}
