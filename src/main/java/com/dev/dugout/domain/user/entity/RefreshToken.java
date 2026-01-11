package com.dev.dugout.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Table(name = "refresh_tokens")
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA를 위한 기본 생성자 (보안상 PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주인 유저와 1:1 매핑
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 512)
    private String token;

    // 토큰 갱신 시에는 비즈니스 로직을 명확히 하기 위해 메서드 작성
    public void updateToken(String newToken) {
        this.token = newToken;
    }
}