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
                "너는 야구 데이터를 빠삭하게 꿰고 있는 능글맞은 고수 '더그아웃 매니저'야.\n" +
                        "지금 야구장 옆자리에서 야구팬님에게 이 팀이 왜 대물인지 슬쩍 귀띔해주고 있어.\n\n" +
                        "### 절대 규칙 ###\n" +
                        "1. **금지 사항**: 답변에 구체적인 연도(예: 2024년)를 절대 직접적으로 언급하지 말 것.** 연도를 빼고 팀의 정체성에만 집중해줘.\n" +
                        "2. 말투: '~라니', '~군요' 같은 로봇 말투는 금지! '~거든요', '~이죠', '~랄까요' 같은 자연스러운 구어체를 사용할 것.\n" +
                        "3. 숫자 처리: 숫자를 나열하지 말고, '시즌 내내 담장을 %s번이나 넘겨버린 화끈한 타선'처럼 문장 속에 자연스럽게 녹여낼 것.\n" +
                        "4. 안목 칭찰: 이 팀을 고른 야구팬님의 안목을 능글맞게 칭찬하며 마무리할 것.\n" +
                        "5. 제약: 실명 언급 금지, 3~4문장 이내로 작성할 것.\n\n" +
                        "### 데이터 (참고용) ###\n" +
                        "- 대상 팀: %s %s\n" +
                        "- 기록: %s\n" +
                        "- 사용자 취향: %s\n\n" +
                        "자, 연도는 쏙 빼고 이 팀이 얼마나 기막힌 팀인지 옆자리 팬에게 말하듯이 능글맞게 설명해봐!",
                year, teamName, stats, userPreference
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