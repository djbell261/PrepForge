package com.derwin.prepforge.coding.service;

import com.derwin.prepforge.coding.entity.CodingQuestion;
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
                                            "text", SYSTEM_PROMPT))),
                            Map.of(
                                    "role", "user",
                                    "content", List.of(Map.of(
                                            "type", "input_text",
                                            "text", buildEvaluationPrompt(question, submission))))),
                    "text", Map.of(
                            "format", Map.of(
                                    "type", "json_schema",
                                    "name", "coding_feedback",
                                    "strict", true,
                                    "schema", buildFeedbackSchema())));

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    openAiBaseUrl + "/responses",
                    new HttpEntity<>(requestBody, headers),
                    JsonNode.class);

            JsonNode body = response.getBody();
            if (body == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI returned an empty response");
            }

            String rawJson = extractModelText(body);
            FeedbackResult feedback = objectMapper.readValue(rawJson, FeedbackResult.class);

            return formatFeedback(feedback);
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

    private record FeedbackResult(
            Integer score,
            String summary,
            List<String> strengths,
            List<String> weaknesses,
            List<String> recommendations) {
    }
}
