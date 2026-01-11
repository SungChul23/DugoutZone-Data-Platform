package com.dev.dugout.domain.team.controller;


import com.dev.dugout.domain.team.dto.GameResponseDto;
import com.dev.dugout.domain.team.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    @GetMapping
    public ResponseEntity<List<GameResponseDto>> getSchedule(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam int month) {

        List<GameResponseDto> schedule = gameService.getMonthlySchedule(year, month);
        return ResponseEntity.ok(schedule);
    }
}
