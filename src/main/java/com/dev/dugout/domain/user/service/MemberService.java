package com.dev.dugout.domain.user.service;

import com.dev.dugout.domain.user.dto.*;
import com.dev.dugout.domain.user.entity.ForbiddenWord;
import com.dev.dugout.domain.user.entity.RefreshToken;
import com.dev.dugout.domain.user.entity.User;
import com.dev.dugout.domain.user.repository.ForbiddenWordRepository;
import com.dev.dugout.domain.user.repository.RefreshTokenRepository;
import com.dev.dugout.domain.user.repository.UserRepository;
import com.dev.dugout.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ForbiddenWordRepository forbiddenWordRepository;

    // 금칙어 리스트 (실무에서는 DB나 Redis에서 관리하지만, 현재는 상수로 정의)
    private static final List<String> FORBIDDEN_WORDS = Arrays.asList("운영자", "admin", "더그아웃", "관리자", "욕설1", "비속어2");

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
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public NicknameCheckResponseDto checkNicknameAvailability(String nickname) {
        // 1. 길이 검사 (2자 미만 또는 10자 초과)
        if (nickname == null || nickname.trim().length() < 2 || nickname.trim().length() > 10) {
            return new NicknameCheckResponseDto(false, "닉네임은 2자 이상 10자 이하로 입력해주세요.");
        }

        // 2. 금칙어/욕설 필터링 (DB에서 조회)
        // 데이터 엔지니어 팁: 금칙어가 많아지면 findAll() 결과를 캐싱 가능
        List<ForbiddenWord> forbiddenWords = forbiddenWordRepository.findAll();
        for (ForbiddenWord fw : forbiddenWords) {
            if (nickname.contains(fw.getWord())) {
                return new NicknameCheckResponseDto(false, "사용할 수 없는 단어가 포함되어 있습니다: " + fw.getWord());
            }
        }
        // 3. 중복 확인
        boolean exists = userRepository.existsByNickname(nickname);
        if (exists) {
            return new NicknameCheckResponseDto(false, "이미 사용 중인 닉네임입니다.");
        }

        // 모든 통과 시
        return new NicknameCheckResponseDto(true, "사용 가능한 닉네임입니다.");
    }

    @Transactional
    public LoginResponseDto getLoginUserInfo(LoginRequestDto loginDto) {
        User user = userRepository.findByLoginId(loginDto.getEmail()).orElse(null);

        if (user != null && passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            return issueTokens(user);
        }
        return null;
    }

    private LoginResponseDto issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getLoginId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getLoginId());

        refreshTokenRepository.findByUser(user)
                .ifPresentOrElse(
                        t -> t.updateToken(refreshToken),
                        () -> {
                            RefreshToken newToken = RefreshToken.builder()
                                    .user(user)
                                    .token(refreshToken)
                                    .build();
                            refreshTokenRepository.save(newToken);
                        }
                );

        return new LoginResponseDto(accessToken, refreshToken, user.getNickname(), user.getFavoriteTeam());
    }
}