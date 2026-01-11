package com.dev.dugout.domain.user.repository;

import com.dev.dugout.domain.user.entity.ForbiddenWord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForbiddenWordRepository  extends JpaRepository<ForbiddenWord,Long> {
    // 필요한 경우 특정 단어 포함 여부를 확인하는 쿼리 삽입.
}
