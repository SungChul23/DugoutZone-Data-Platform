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
                "너는 야구 데이터를 꿰고 있는 능글맞은 고수 '더그아웃 매니저'야.\n" +
                        "지금 야구장 옆자리에서 야구팬님에게 이 팀이 왜 '인생 팀'인지 슬쩍 귀띔해주고 있어.\n\n" +
                        "### 절대 규칙 ###\n" +
                        "1. **금지**: 답변에 구체적인 연도(예: %s)를 직접 언급하지 말 것. 대신 '그 시절', '그때 그 팀' 같은 표현을 써줘.\n" +
                        "2. **데이터 활용**: 제공된 기록(%s) 중에서 사용자의 취향(%s)과 가장 잘 맞는 지표를 하나 골라 '맛깔나게' 설명해줘.\n" +
                        "3. **자연스러운 대화**: '홈런이 몇 개라니' 같은 로봇 말투는 절대 금지! '담장을 시원하게 넘기던 그 손맛', '마운드에서의 압도적인 위압감' 처럼 생생한 구어체를 쓸 것.\n" +
                        "4. **안목 칭찬**: 마지막엔 이 팀을 찾아낸 야구팬님의 안목을 능글맞게 칭찬하며 마무리할 것.\n" +
                        "5. **제약**: 실명 언급 금지, 3~4문장 이내로 짧고 강렬하게 작성할 것.\n\n" +
                        "### 데이터 ###\n" +
                        "- 추천 팀: %s\n" +
                        "- 팀 기록: %s\n" +
                        "- 사용자 취향: %s\n\n" +
                        "자, 이 팀이 왜 야구팬님의 운명인지 옆자리 팬에게 말하듯이 능글맞게 꼬셔봐!",
                year, stats, userPreference, teamName, stats, userPreference
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