package com.dev.dugout.infrastructure.aws.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Getter
@Setter
public class SurveyRequest {
    private int startYear; // 어느년도 부터 야구를 보고 시작하셧나요
    private Map<String, Integer> preferences; // q1~q6의 질문 및 점수 매핑
}
