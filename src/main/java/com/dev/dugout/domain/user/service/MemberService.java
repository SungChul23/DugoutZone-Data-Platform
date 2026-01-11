package com.dev.dugout.domain.user.service;

import com.dev.dugout.domain.user.dto.LoginRequestDto;
import com.dev.dugout.domain.user.dto.LoginResponseDto;
import com.dev.dugout.domain.user.dto.SignupRequestDto;
import com.dev.dugout.domain.user.entity.RefreshToken;
import com.dev.dugout.domain.user.entity.User;
import com.dev.dugout.domain.user.repository.RefreshTokenRepository;
import com.dev.dugout.domain.user.repository.UserRepository;
import com.dev.dugout.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // 1. 회원가입: 반환 타입을 LoginResponseDto로 변경 (자동 로그인용)
    @Transactional
    public LoginResponseDto signup(SignupRequestDto requestDto) {
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        User user = User.builder()
                .loginId(requestDto.getEmail())
                .password(encodedPassword)
                .nickname(requestDto.getNickname())
                .favoriteTeam(requestDto.getFavoriteTeam())
                .build();

        userRepository.save(user);

        // 가입 성공 후 바로 토큰 발급 로직 호출
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    // 2. 로그인 로직: 직접 DTO를 만들지 않고 issueTokens 활용
    @Transactional
    public LoginResponseDto getLoginUserInfo(LoginRequestDto loginDto) {
        User user = userRepository.findByLoginId(loginDto.getEmail()).orElse(null);

        if (user != null && passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            // 수정됨: 4개의 인자를 받는 DTO를 직접 생성하지 않고 메서드 호출
            return issueTokens(user);
        }
        return null;
    }

    // 3. 토큰 발급 공통 로직 (Refresh Token DB 저장 포함)
    private LoginResponseDto issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getLoginId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getLoginId());

        // 리프레시 토큰 DB 저장/업데이트
        refreshTokenRepository.findByUser(user)
                .ifPresentOrElse(
                        t -> t.updateToken(refreshToken),
                        () -> {
                            // 중괄호를 추가하여 반환값 없이 실행만 하도록 명시 (에러 방지)
                            RefreshToken newToken = RefreshToken.builder()
                                    .user(user)
                                    .token(refreshToken)
                                    .build();
                            refreshTokenRepository.save(newToken);
                        }
                );

        // 최종적으로 4개의 인자를 담은 DTO 반환
        return new LoginResponseDto(accessToken, refreshToken, user.getNickname(), user.getFavoriteTeam());
    }
}