package com.dev.dugout.domain.team.service;

import com.dev.dugout.domain.team.entity.Game;
import com.dev.dugout.domain.team.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.dev.dugout.domain.team.dto.GameResponseDto;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;

    //월별 경기 일정 조회 메서드
    public List<GameResponseDto> getMonthlySchedule(int year, int month) {
        //조회 기간 계산
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        //DB에서 경기 조회
        List<Game> games = gameRepository.findMonthlySchedule(startDate, endDate);

        //엔티티 → DTO 변환
        return games.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    //API 응답 전용 DTO 생성
    private GameResponseDto convertToResponse(Game game) {
        return GameResponseDto.builder()
                .id(game.getId())
                .date(game.getGameDate().toString()) // YYYY-MM-DD
                .time(game.getGameTime().toString().substring(0, 5)) // HH:mm:ss -> HH:mm
                .home(game.getHomeTeam().getName()) // 구단 풀네임
                .away(game.getAwayTeam().getName()) // 구단 풀네임
                .stadium(game.getStadiumName())
                .status(game.getStatus() != null ? game.getStatus() : "SCHEDULED")
                .build();
    }
}
