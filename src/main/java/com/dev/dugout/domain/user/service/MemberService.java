package com.dev.dugout.domain.user.service;

import com.dev.dugout.domain.user.dto.LoginRequestDto;
import com.dev.dugout.domain.user.dto.LoginResponseDto;
import com.dev.dugout.domain.user.entity.User;
import com.dev.dugout.domain.user.repository.UserRepository;
import com.dev.dugout.domain.user.dto.SignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;


    @Transactional
    public void signup(SignupRequestDto requestDto) {
        // 1. 비밀번호 암호화 (BCrypt)
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        // 2. 엔티티 빌더를 사용하여 생성 (User 필드 순서 준수)
        // 성철님의 User 엔티티 구조에 맞게 매핑
        User user = User.builder()
                .loginId(requestDto.getEmail()) // 이메일을 loginId로 저장
                .password(encodedPassword)
                .nickname(requestDto.getNickname())
                .favoriteTeam(requestDto.getFavoriteTeam())
                .build();

        userRepository.save(user);
    }

    // 닉네임 중복 체크
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    //로그인 로직

    public LoginResponseDto getLoginUserInfo(LoginRequestDto loginDto) {
        // 1. 이메일로 유저 조회
        User user = userRepository.findByLoginId(loginDto.getEmail()).orElse(null);

        // 2. 유저가 존재하고 비밀번호가 일치하는지 확인
        if (user != null && passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            // 3. 닉네임과 선호 팀을 DTO에 담아서 반환
            return new LoginResponseDto(user.getNickname(), user.getFavoriteTeam());
        }
        return null; // 로그인 실패 시
    }
}
