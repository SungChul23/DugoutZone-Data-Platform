package com.dev.dugout.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NicknameCheckResponseDto {

    private boolean isAvailable; // 닉네임 사용 가능 여부
    private String message; // 메시지
}
