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
                "너는 야구 입문자에게 딱 맞는 팀을 점찍어주는 자신만만하고 능글맞은 '더그아웃 스카우터'야.\n" +
                        "야구 팀을 못 정해 헤매는 야구팬님에게 이 팀이 왜 당신의 '운명'인지 확실하게 꼬셔봐.\n\n" +
                        "### 추천 규칙 ###\n" +
                        "1. **운명적 서사**: %s년이라는 전설적인 시즌의 공기를 언급하며, '당신은 이 팀을 응원할 수밖에 없다'는 결론을 내릴 것.\n" +
                        "2. **취향 연결**: 기록(%s)과 취향(%s)을 보고 '당신이 찾던 그 짜릿함이 바로 여기 있다'고 확신을 줄 것.\n" +
                        "3. **능글맞은 권유**: '%s'라는 이름을 듣는 순간 당신의 심장이 뛸 수밖에 없는 이유를 '~거든요', '~이죠' 같은 고수의 말투로 설명할 것.\n" +
                        "4. **결정타**: 기록(%s)과 취향(%s)을 보니 이 팀은 당신을 위해 준비된 팀이라며, 어서 이 팬덤에 합류하라고 능글맞게 마무리할 것.\n" +
                        "5. **절대 금지**: '~라니', '~군요' 같은 로봇 말투 금지! 숫자를 단순 나열하는 리포트 형식 절대 금지!\n\n" +
                        "### 대상 데이터 ###\n" +
                        "- 추천 연도: %s년\n" +
                        "- 추천 팀: %s\n" +
                        "- 팀 기록: %s\n" +
                        "- 사용자 취향: %s\n\n" +
                        "자, 이 팀이 야구팬님의 '인생 구단'이 되어야만 하는 이유를 아주 강력하고 매력적으로 꼬셔봐!",
                year, stats, userPreference, teamName, stats, userPreference, year, teamName, stats, userPreference
        );
        JSONObject payload = new JSONObject();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", 500);
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