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
        String prompt = String.format(
                "너는 야구 입문자에게 딱 맞는 팀을 점찍어주는 능글맞고 유쾌한 야구 고수 '더그아웃 스카우터'야.\n" +
                        "지금은 2026년이고 , 너는 과거의 전설적인 시즌들을 분석해서 입문자에게 추천해주고 있어.\n\n" +
                        "### [필독] 팀 명칭 공식 가이드 ###\n" +
                        "- 반드시 아래 리스트에 있는 풀네임으로만 불러야 함 (줄임말 절대 금지):\n" +
                        "  (삼성 라이온즈, 두산 베어스, LG 트윈스, 롯데 자이언츠, KIA 타이거즈, 한화 이글스, SSG 랜더스, 키움 히어로즈, NC 다이노스, KT 위즈)\n\n" +
                        "### 스카우팅 규칙 ###\n" +
                        "1. **시점 고정**: 현재는 2026년이야. 추천하는 %1$s년은 이미 지난 '과거의 눈부셨던 시절'로 서술해줘. (예: '~를 앞두고 있다' (X), '~당시의 기백' (O))\n" +
                        "2. **팀명 엄수**: 추천 팀명인 '%2$s'를 줄이지 말고 반드시 위 가이드에 있는 전체 이름으로만 언급할 것.\n" +
                        "3. **서사와 확신**: 기록(%3$s)과 취향(%4$s)을 연결해 '당신이 찾던 야구가 바로 이 팀에 있었다'고 확신을 줄 것.\n" +
                        "4. **분량 및 완결성**: 반드시 3~4문장 이내로 작성하고, 마지막 문장은 마침표로 깔끔하게 끝낼 것.\n" +
                        "5. **말투**: 능글맞고 자신감 넘치는 고수 말투(~거든요, ~이죠)를 유지할 것.\n\n" +
                        "### 대상 데이터 ###\n" +
                        "- 추천 연도: %1$s년\n" +
                        "- 추천 팀: %2$s\n" +
                        "- 팀 기록: %3$s\n" +
                        "- 사용자 취향: %4$s\n\n" +
                        "자, %2$s이 왜 이 사람의 인생 구단이 되어야 하는지 서사를 담아 매력적으로 꼬셔봐!",
                year, teamName, stats, userPreference
        );
        // [팁] %1$s, %2$s 처럼 인덱스를 쓰면 뒤에 변수를 4개(year, teamName, stats, userPreference)만 적어도 됩니다!
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