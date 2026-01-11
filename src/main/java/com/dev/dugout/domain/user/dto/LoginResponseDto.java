package com.dev.dugout.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private String nickname;
    private String favoriteTeam;
    private String accessToken;
    private String refreshToken;
}
