package com.dev.dugout.infrastructure.aws.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class BedrockService {

    private final BedrockRuntimeClient client = BedrockRuntimeClient.builder()
            .region(Region.AP_NORTHEAST_2) // 서울 리전
            .build();

    public String generateReason(String teamName, String year, String stats, String userPreference) {
        // [가드레일 및 페르소나 강화 프롬프트]
        String prompt = String.format(
                "너는 야구 데이터를 빠삭하게 꿰고 있는 능글맞고 유쾌한 야구 고수 '더그아웃 매니저'야.\n" +
                        "아래 규칙을 지켜서 사용자가 무릎을 탁 칠 정도로 맛깔나게 답변해줘.\n\n" +
                        "### 규칙 ###\n" +
                        "1. 페르소나: 약간은 능글맞으면서도 자신감 넘치는 말투를 쓸 것. (예: '오호, 이거 보세요', '이건 못 참죠', '기막힌 선택이네요')\n" +
                        "2. 역사적 별명: 해당 팀의 전설적인 별명(예: 홈런 공장, 왕조, 육성광산 등)을 반드시 언급할 것.\n" +
                        "3. 프랜차이즈 스타: 해당 연도 그 팀의 상징적인 선수 1~2명의 이름을 언급하며 기대감을 높일 것.\n" +
                        "4. 보안: 사용자의 실명은 절대 언급 금지! '야구팬님' 혹은 '당신'이라고 부를 것.\n" +
                        "5. 제약: 제공된 데이터(기록)를 기반으로 하되, 3~4문장 이내로 작성할 것.\n\n" +
                        "### 데이터 ###\n" +
                        "- 추천 팀: %s년 %s\n" +
                        "- 팀 기록: %s\n" +
                        "- 사용자 취향: %s\n\n" +
                        "자, 이 팀이 왜 야구팬님의 운명인지 '능글맞게' 한 번 꼬셔봐!",
                year, teamName, stats, userPreference
        );

        JSONObject payload = new JSONObject();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", 250);
        payload.put("temperature", 0.6); // 안정성을 위해 살짝 낮춤

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "user").put("content", prompt));
        payload.put("messages", messages);

        try {
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId("anthropic.claude-3-haiku-20240307-v1:0")
                    .contentType("application/json")
                    .body(SdkBytes.fromUtf8String(payload.toString()))
                    .build();

            InvokeModelResponse response = client.invokeModel(request);
            JSONObject resp = new JSONObject(response.body().asUtf8String());
            return resp.getJSONArray("content").getJSONObject(0).getString("text");
        } catch (Exception e) {
            log.error(">>>> [BEDROCK ERROR] 원인: {}", e.getMessage()); // 로그에 에러 찍기
            e.printStackTrace(); // 상세 스택트레이스 출력
            return "AI 분석 실패 원인: " + e.getMessage();
        }
    }
}