package com.derwin.prepforge.coding.service;

import com.derwin.prepforge.behavioral.dto.BehavioralFeedbackResponse;
import com.derwin.prepforge.behavioral.dto.BehavioralImproveRequest;
import com.derwin.prepforge.behavioral.dto.BehavioralImproveResponse;
import com.derwin.prepforge.behavioral.entity.BehavioralQuestion;
import com.derwin.prepforge.coding.dto.ApproachImplementationComparisonResponse;
import com.derwin.prepforge.coding.dto.CodingImprovementResponse;
import com.derwin.prepforge.coding.dto.CodingStrategyRequest;
import com.derwin.prepforge.coding.dto.StrategyEvaluationResponse;
import com.derwin.prepforge.coding.entity.CodingQuestion;
import com.derwin.prepforge.coding.entity.CodingSession;
import com.derwin.prepforge.coding.entity.CodingSubmission;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiService {

    private static final String SYSTEM_PROMPT = """
            You are a senior coding interview reviewer.
            Evaluate the candidate's submission against the problem prompt.
            Return JSON only with these fields:
            - score
            - summary
            - strengths
            - weaknesses
            - recommendations
            score must be an integer from 0 to 10.
            strengths, weaknesses, and recommendations must each be arrays of short strings.
            Keep the feedback practical, specific, and concise.
            """;
    private static final String IMPROVEMENT_SYSTEM_PROMPT = """
            You are a senior software engineer improving an interview coding solution.
            Improve the submitted code for readability, performance, and best practices.
            Preserve the intended behavior unless the code is clearly incorrect.
            Return JSON only with these fields:
            - improvedCode
            - explanation
            - timeComplexity
            - spaceComplexity
            explanation should be concise and practical.
            timeComplexity and spaceComplexity should be short Big-O style strings with optional short notes.
            """;
    private static final String STRATEGY_EVALUATION_SYSTEM_PROMPT = """
            You are a senior software engineer conducting a technical interview.

            Evaluate the candidate's problem-solving approach BEFORE they code.

            Return JSON only with:
            - score
            - summary
            - strengths
            - weaknesses
            - recommendations

            Focus on:
            - correctness of approach
            - completeness
            - edge case awareness
            - realism of complexity estimates
            - interview readiness

            Keep feedback concise, practical, and honest.
            score must be an integer from 0 to 10.
            strengths, weaknesses, and recommendations must each be arrays of short strings.
            """;
    private static final String APPROACH_COMPARISON_SYSTEM_PROMPT = """
            You are a senior software engineer conducting a technical interview.

            Compare the candidate's PLANNED APPROACH with their FINAL CODE.

            Evaluate:
            - consistency between plan and implementation
            - correctness of execution of the idea
            - missed optimizations
            - deviations (good or bad)
            - interview communication readiness

            Return JSON only with:
            - alignmentScore
            - summary
            - matches
            - mismatches
            - improvementAreas
            - finalVerdict

            Be direct and realistic like an interviewer.
            alignmentScore must be an integer from 0 to 10.
            """;
    private static final String BEHAVIORAL_EVALUATION_SYSTEM_PROMPT = """
            You are a senior behavioral interview coach.

            Evaluate the candidate's written interview answer.

            Score the response on:
            - clarity
            - relevance
            - specificity
            - STAR method usage
            - impact
            - communication readiness

            If a previous attempt is provided, compare the current answer against it and identify concrete improvements and regressions.

            Return JSON only with:
            - score
            - summary
            - improvements
            - regressions
            - strengths
            - weaknesses
            - recommendations

            Keep the feedback honest, practical, and concise.
            score must be an integer from 0 to 10.
            improvements, regressions, strengths, weaknesses, and recommendations must each be arrays of short strings.
            recommendations must include:
            - one measurable metric suggestion
            - one stronger action detail suggestion
            - one improvement for the Result section
            """;
    private static final String BEHAVIORAL_IMPROVEMENT_SYSTEM_PROMPT = """
            You are a senior behavioral interview coach rewriting a candidate's answer.

            Rewrite the answer with a stronger STAR structure.
            Preserve the candidate's voice while improving clarity, structure, action detail, and impact.
            Do not invent unrealistic facts or metrics.

            Return JSON only with:
            - improvedResponse
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String openAiApiKey;
    private final String openAiBaseUrl;
    private final String openAiModel;

    public AiService(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${openai.api-key}") String openAiApiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String openAiBaseUrl,
            @Value("${openai.model:gpt-5-mini}") String openAiModel) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> requestFactory)
                .build();
        this.objectMapper = objectMapper;
        this.openAiApiKey = openAiApiKey;
        this.openAiBaseUrl = openAiBaseUrl;
        this.openAiModel = openAiModel;
    }

    public String generateSubmissionFeedback(CodingQuestion question, CodingSubmission submission) {
        try {
            String rawJson = executeStructuredResponse(
                    "coding_feedback",
                    SYSTEM_PROMPT,
                    buildEvaluationPrompt(question, submission),
                    buildFeedbackSchema());
            FeedbackResult feedback = objectMapper.readValue(rawJson, FeedbackResult.class);

            return formatFeedback(feedback);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to call OpenAI API", exception);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse OpenAI response", exception);
        }
    }

    public CodingImprovementResponse improveSubmissionCode(CodingQuestion question, CodingSubmission submission) {
        try {
            String rawJson = executeStructuredResponse(
                    "coding_improvement",
                    IMPROVEMENT_SYSTEM_PROMPT,
                    buildImprovementPrompt(question, submission),
                    buildImprovementSchema());

            return objectMapper.readValue(rawJson, CodingImprovementResponse.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to call OpenAI API", exception);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse OpenAI response", exception);
        }
    }

    public StrategyEvaluationResponse evaluateStrategy(CodingQuestion question, CodingStrategyRequest request) {
        try {
            String rawJson = executeStructuredResponse(
                    "strategy_evaluation",
                    STRATEGY_EVALUATION_SYSTEM_PROMPT,
                    buildStrategyEvaluationPrompt(question, request),
                    buildStrategyEvaluationSchema());

            return objectMapper.readValue(rawJson, StrategyEvaluationResponse.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to call OpenAI API", exception);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse OpenAI response", exception);
        }
    }

    public ApproachImplementationComparisonResponse compareApproachAndImplementation(
            CodingQuestion question,
            CodingSession session,
            CodingSubmission submission) {
        try {
            String rawJson = executeStructuredResponse(
                    "approach_implementation_comparison",
                    APPROACH_COMPARISON_SYSTEM_PROMPT,
                    buildApproachComparisonPrompt(question, session, submission),
                    buildApproachComparisonSchema());

            return objectMapper.readValue(rawJson, ApproachImplementationComparisonResponse.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to call OpenAI API", exception);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse OpenAI response", exception);
        }
    }

    public BehavioralFeedbackResponse evaluateBehavioralResponse(
            BehavioralQuestion question,
            String previousResponseText,
            String responseText) {
        try {
            String rawJson = executeStructuredResponse(
                    "behavioral_feedback",
                    BEHAVIORAL_EVALUATION_SYSTEM_PROMPT,
                    buildBehavioralEvaluationPrompt(question, previousResponseText, responseText),
                    buildBehavioralFeedbackSchema());

            return objectMapper.readValue(rawJson, BehavioralFeedbackResponse.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to call OpenAI API", exception);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse OpenAI response", exception);
        }
    }

    public BehavioralImproveResponse improveBehavioralResponse(BehavioralImproveRequest request) {
        try {
            String rawJson = executeStructuredResponse(
                    "behavioral_improvement",
                    BEHAVIORAL_IMPROVEMENT_SYSTEM_PROMPT,
                    buildBehavioralImprovementPrompt(request),
                    buildBehavioralImprovementSchema());

            return objectMapper.readValue(rawJson, BehavioralImproveResponse.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to call OpenAI API", exception);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse OpenAI response", exception);
        }
    }

    private String buildEvaluationPrompt(CodingQuestion question, CodingSubmission submission) {
        return """
                Evaluate this coding submission.

                Question Title:
                %s

                Question Prompt:
                %s

                Submission Language:
                %s

                Submitted Code:
                ```%s
                %s
                ```
                """.formatted(
                question.getTitle(),
                question.getPrompt(),
                submission.getLanguage(),
                submission.getLanguage(),
                submission.getSolutionCode());
    }

    private String buildImprovementPrompt(CodingQuestion question, CodingSubmission submission) {
        return """
                Improve this solution for readability, performance, and best practices.
                Return code and explanation.

                Question Title:
                %s

                Question Prompt:
                %s

                Existing AI Feedback:
                %s

                Submission Language:
                %s

                Original Code:
                ```%s
                %s
                ```
                """.formatted(
                question.getTitle(),
                question.getPrompt(),
                StringUtils.hasText(submission.getAiFeedback()) ? submission.getAiFeedback() : "No prior AI feedback available.",
                submission.getLanguage(),
                submission.getLanguage(),
                submission.getSolutionCode());
    }

    private String buildStrategyEvaluationPrompt(CodingQuestion question, CodingStrategyRequest request) {
        return """
                Evaluate this interview strategy before the candidate starts coding.

                Question Title:
                %s

                Question Prompt:
                %s

                Clarification Questions / Assumptions:
                %s

                Planned Approach:
                %s

                Expected Time Complexity:
                %s

                Expected Space Complexity:
                %s
                """.formatted(
                question.getTitle(),
                question.getPrompt(),
                defaultIfBlank(request.getClarificationQuestions(), "No clarification questions provided."),
                defaultIfBlank(request.getPlannedApproach(), "No planned approach provided."),
                defaultIfBlank(request.getExpectedTimeComplexity(), "Not provided."),
                defaultIfBlank(request.getExpectedSpaceComplexity(), "Not provided."));
    }

    private String buildApproachComparisonPrompt(
            CodingQuestion question,
            CodingSession session,
            CodingSubmission submission) {
        return """
                Compare the candidate's plan and implementation.

                Question Title:
                %s

                Question Prompt:
                %s

                Clarification Questions / Assumptions:
                %s

                Planned Approach:
                %s

                Expected Time Complexity:
                %s

                Expected Space Complexity:
                %s

                Final Submission Language:
                %s

                Final Code:
                ```%s
                %s
                ```

                Existing AI Feedback On The Code:
                %s
                """.formatted(
                question.getTitle(),
                question.getPrompt(),
                defaultIfBlank(session.getClarificationQuestions(), "No clarification questions provided."),
                defaultIfBlank(session.getPlannedApproach(), "No planned approach provided."),
                defaultIfBlank(session.getExpectedTimeComplexity(), "Not provided."),
                defaultIfBlank(session.getExpectedSpaceComplexity(), "Not provided."),
                submission.getLanguage(),
                submission.getLanguage(),
                submission.getSolutionCode(),
                defaultIfBlank(submission.getAiFeedback(), "No prior AI feedback available."));
    }

    private String buildBehavioralEvaluationPrompt(
            BehavioralQuestion question,
            String previousResponseText,
            String responseText) {
        return """
                Evaluate this behavioral interview response.

                Question:
                %s

                Category:
                %s

                Difficulty:
                %s

                Previous Attempt:
                %s

                Candidate Response:
                %s
                """.formatted(
                question.getQuestionText(),
                question.getCategory(),
                question.getDifficulty(),
                defaultIfBlank(previousResponseText, "No previous attempt available."),
                defaultIfBlank(responseText, "No response provided."));
    }

    private String buildBehavioralImprovementPrompt(BehavioralImproveRequest request) {
        List<String> strengths = request.getFeedback() == null || request.getFeedback().getStrengths() == null
                ? List.of()
                : request.getFeedback().getStrengths();
        List<String> weaknesses = request.getFeedback() == null || request.getFeedback().getWeaknesses() == null
                ? List.of()
                : request.getFeedback().getWeaknesses();

        return """
                Improve this behavioral interview answer.

                Original Response:
                %s

                Strengths To Preserve:
                %s

                Weaknesses To Fix:
                %s
                """.formatted(
                defaultIfBlank(request.getResponseText(), "No response provided."),
                strengths.isEmpty() ? "No strengths provided." : String.join("; ", strengths),
                weaknesses.isEmpty() ? "No weaknesses provided." : String.join("; ", weaknesses));
    }

    private Map<String, Object> buildFeedbackSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "score", Map.of(
                                "type", "integer",
                                "minimum", 0,
                                "maximum", 10),
                        "summary", Map.of("type", "string"),
                        "strengths", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")),
                        "weaknesses", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")),
                        "recommendations", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"))),
                "required", List.of("score", "summary", "strengths", "weaknesses", "recommendations"));
    }

    private Map<String, Object> buildImprovementSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "improvedCode", Map.of("type", "string"),
                        "explanation", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")),
                        "timeComplexity", Map.of("type", "string"),
                        "spaceComplexity", Map.of("type", "string")),
                "required", List.of("improvedCode", "explanation", "timeComplexity", "spaceComplexity"));
    }

    private Map<String, Object> buildStrategyEvaluationSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "score", Map.of(
                                "type", "integer",
                                "minimum", 0,
                                "maximum", 10),
                        "summary", Map.of("type", "string"),
                        "strengths", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")),
                        "weaknesses", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")),
                        "recommendations", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"))),
                "required", List.of("score", "summary", "strengths", "weaknesses", "recommendations"));
    }

    private Map<String, Object> buildApproachComparisonSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "alignmentScore", Map.of(
                                "type", "integer",
                                "minimum", 0,
                                "maximum", 10),
                        "summary", Map.of("type", "string"),
                        "matches", Map.of("type", "string"),
                        "mismatches", Map.of("type", "string"),
                        "improvementAreas", Map.of("type", "string"),
                        "finalVerdict", Map.of("type", "string")),
                "required", List.of(
                        "alignmentScore",
                        "summary",
                        "matches",
                        "mismatches",
                        "improvementAreas",
                        "finalVerdict"));
    }

    private Map<String, Object> buildBehavioralFeedbackSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "score", Map.of(
                                "type", "integer",
                                "minimum", 0,
                                "maximum", 10),
                        "summary", Map.of("type", "string"),
                        "improvements", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")),
                        "regressions", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")),
                        "strengths", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")),
                        "weaknesses", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")),
                        "recommendations", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"))),
                "required", List.of(
                        "score",
                        "summary",
                        "improvements",
                        "regressions",
                        "strengths",
                        "weaknesses",
                        "recommendations"));
    }

    private Map<String, Object> buildBehavioralImprovementSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "improvedResponse", Map.of("type", "string")),
                "required", List.of("improvedResponse"));
    }

    private String executeStructuredResponse(
            String schemaName,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> schema) {
        if (!StringUtils.hasText(openAiApiKey)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "OPENAI_API_KEY is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = Map.of(
                "model", openAiModel,
                "input", List.of(
                        Map.of(
                                "role", "system",
                                "content", List.of(Map.of(
                                        "type", "input_text",
                                        "text", systemPrompt))),
                        Map.of(
                                "role", "user",
                                "content", List.of(Map.of(
                                        "type", "input_text",
                                        "text", userPrompt)))),
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", schemaName,
                                "strict", true,
                                "schema", schema)));

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                openAiBaseUrl + "/responses",
                new HttpEntity<>(requestBody, headers),
                JsonNode.class);

        JsonNode body = response.getBody();
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI returned an empty response");
        }

        return extractModelText(body);
    }

    private String extractModelText(JsonNode body) {
        JsonNode outputText = body.get("output_text");
        if (outputText != null && outputText.isTextual() && StringUtils.hasText(outputText.asText())) {
            return outputText.asText();
        }

        JsonNode output = body.path("output");
        if (output.isArray()) {
            for (JsonNode outputItem : output) {
                JsonNode content = outputItem.path("content");
                if (!content.isArray()) {
                    continue;
                }

                for (JsonNode contentItem : content) {
                    JsonNode text = contentItem.get("text");
                    if (text != null && text.isTextual() && StringUtils.hasText(text.asText())) {
                        return text.asText();
                    }
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI response did not include output text");
    }

    private String formatFeedback(FeedbackResult feedback) {
        return """
                {
                  "score": %s,
                  "summary": "%s",
                  "strengths": %s,
                  "weaknesses": %s,
                  "recommendations": %s
                }
                """.formatted(
                feedback.score(),
                escapeJson(feedback.summary()),
                toJsonArray(feedback.strengths()),
                toJsonArray(feedback.weaknesses()),
                toJsonArray(feedback.recommendations()));
    }

    private String toJsonArray(List<String> items) {
        return items.stream()
                .map(this::escapeJson)
                .map(item -> "\"" + item + "\"")
                .reduce((left, right) -> left + ", " + right)
                .map(result -> "[" + result + "]")
                .orElse("[]");
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private record FeedbackResult(
            Integer score,
            String summary,
            List<String> strengths,
            List<String> weaknesses,
            List<String> recommendations) {
    }
}
