package com.dev.dugout.infrastructure.aws.service;


import com.dev.dugout.infrastructure.aws.dto.SurveyRequest;
import com.dev.dugout.infrastructure.aws.dto.TeamRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

import java.util.List;
import java.util.Map;

//Min-Max Scaling (정규화) 적용

//사용자 설문 점수 → 가중치 계산 → Athena(SQL)로 분석 → 최고 점수 팀 1개 추천
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final AthenaClient athenaClient;

    @Value("${aws.athena.database}")
    private String database;
    @Value("${aws.athena.output-location}")
    private String outputLocation;

    // 팀 풀네임 매핑 테이블
    private static final Map<String, String> FULL_TEAM_NAMES = Map.ofEntries(
            Map.entry("삼성", "삼성 라이온즈"), Map.entry("두산", "두산 베어스"),
            Map.entry("LG", "LG 트윈스"), Map.entry("롯데", "롯데 자이언츠"),
            Map.entry("KIA", "KIA 타이거즈"), Map.entry("한화", "한화 이글스"),
            Map.entry("SSG", "SSG 랜더스"), Map.entry("키움", "키움 히어로즈"),
            Map.entry("NC", "NC 다이노스"), Map.entry("KT", "kt wiz"),
            Map.entry("현대", "현대 유니콘스"), Map.entry("SK", "SK 와이번스"),
            Map.entry("넥센", "넥센 히어로즈")
    );

    public TeamRecommendationResponse getMatchTeam(SurveyRequest request) {
        // 가중치 계산 (1~5 -> 0.2~1.0)
        double w1 = request.getPreferences().get("q1") / 5.0;
        double w2 = request.getPreferences().get("q2") / 5.0;
        double w3 = request.getPreferences().get("q3") / 5.0;
        double w4 = request.getPreferences().get("q4") / 5.0;
        double w5 = request.getPreferences().get("q5") / 5.0;
        double w6 = request.getPreferences().get("q6") / 5.0;

        String sql = String.format(
                "SELECT h.year, h.\"팀명\", (" +
                        "((CAST(h.hr AS DOUBLE) / 234.0) * %.2f) + " +
                        "((CAST(h.avg AS DOUBLE) / 0.300) * %.2f) + " +
                        "(((5.0 - CAST(p.era AS DOUBLE)) / 5.0) * %.2f) + " +
                        "((CAST(p.sv + p.hld AS DOUBLE) / 140.0) * %.2f) + " +
                        "((CAST(h.ops AS DOUBLE) / 1.0) * %.2f) + " +
                        "((CAST(p.wpct AS DOUBLE) / 1.0) * %.2f)" +
                        ") AS total_score " +
                        "FROM type_hitter h JOIN type_pitcher p ON h.year = p.year AND h.team_id = p.team_id " +
                        "WHERE h.year >= '%d' ORDER BY total_score DESC LIMIT 1",
                w1, w2, w3, w4, w5, w6, request.getStartYear()
        );

        return executeAthenaQuery(sql);
    }


    //데이터 파이프라인 지휘
    //아테나는 비동기라 쿼리 실행후 -> 쿼리 id를 줌 -> id에 대한 폴링을 통해 "다됐냐? 라고 물어봄"
    private TeamRecommendationResponse executeAthenaQuery(String sql) {
        StartQueryExecutionRequest startRequest = StartQueryExecutionRequest.builder()
                .queryString(sql)
                .queryExecutionContext(QueryExecutionContext.builder().database(database).build())
                .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
                .build();

        String executionId = athenaClient.startQueryExecution(startRequest).queryExecutionId();

        // 쿼리 완료 대기
        waitForQuery(executionId);

        GetQueryResultsResponse results = athenaClient.getQueryResults(
                GetQueryResultsRequest.builder().queryExecutionId(executionId).maxResults(2).build()
        );

        List<Row> rows = results.resultSet().rows();
        if (rows.size() > 1) {
            List<Datum> data = rows.get(1).data();
            String dbTeamName = data.get(1).varCharValue(); // 예: "두산"

            return TeamRecommendationResponse.builder()
                    .year(data.get(0).varCharValue())
                    .originalName(dbTeamName)
                    .teamName(FULL_TEAM_NAMES.getOrDefault(dbTeamName, dbTeamName + " 구단")) // 풀네임 변환
                    .score(Double.parseDouble(data.get(2).varCharValue()))
                    .reason(" EX) 분석 완료! 곧 Bedrock AI가 상세 설명을 생성합니다.")
                    .build();
        }
        throw new RuntimeException("No match found");
    }

    //상태 확인(폴링)
    private void waitForQuery(String id) {
        while (true) {
            // GetQueryExecutionRequest를 통해 응답을 받습니다.
            GetQueryExecutionResponse res = athenaClient.getQueryExecution(
                    GetQueryExecutionRequest.builder().queryExecutionId(id).build()
            );

            // queryExecution()을 먼저 호출해야 status()에 접근 가능
            QueryExecutionStatus status = res.queryExecution().status();
            String state = status.state().toString();

            if (state.equals("SUCCEEDED")) {
                return;
            }

            if (state.equals("FAILED") || state.equals("CANCELLED")) {
                throw new RuntimeException("Athena Error: " + status.stateChangeReason());
            }

            try {
                Thread.sleep(500);
            } catch (Exception ignored) {
                Thread.currentThread().interrupt(); // 인터럽트 예외 처리 관례
            }
        }
    }

}