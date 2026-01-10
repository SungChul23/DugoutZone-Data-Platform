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
                "너는 야구 데이터를 기반으로 사용자의 운명적인 팀을 찾아주는 열정적이고 다정한 친구 '더그아웃 AI'야.\n" +
                        "아래 규칙을 반드시 지켜서 답변해줘.\n\n" +
                        "### 규칙 ###\n" +
                        "1. 도입부: '드디어 찾았어요!', '우와!', '데이터를 분석해 보니 이 팀이 딱이네요!' 처럼 사용자의 기대감을 높이는 활기차고 자연스러운 문구로 시작할 것.\n" +
                        "2. 말투: '~해요', '~네요' 체를 사용하여 친근하게 대화하듯 말할 것.\n" +
                        "3. 보안: 사용자의 실명이나 이름을 절대 언급하지 말 것. 대신 '당신' 혹은 '야구팬님'이라고 부를 것.\n" +
                        "4. 가드레일: 제공된 데이터(기록, 취향)에 근거하되, 마치 야구장 옆자리에서 설명해 주는 것처럼 생생하게 묘사할 것.\n" +
                        "5. 분량: 반드시 3문장 이내로 짧고 강렬하게 작성할 것.\n\n" +
                        "### 데이터 ###\n" +
                        "- 추천 팀: %s년 %s\n" +
                        "- 팀 기록: %s\n" +
                        "- 사용자 취향: %s\n\n" +
                        "자, 이 팀이 왜 당신의 운명인지 맛깔나게 설명해줘!",
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