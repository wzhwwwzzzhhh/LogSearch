package com.loganalytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${llm.api-key}")
    private String apiKey;

    @Value("${llm.api-url}")
    private String apiUrl;

    @Value("${llm.model}")
    private String model;

    @Value("${llm.max-tokens}")
    private int maxTokens;

    @Value("${llm.temperature}")
    private double temperature;

    @Value("${llm.prompt.system}")
    private String systemPrompt;

    @Value("${llm.prompt.analysis}")
    private String analysisPrompt;

    public LlmService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generateDsl(String userQuestion) {
        String dsl = callDeepSeek(userQuestion, systemPrompt);
        log.info("LLM生成的原始DSL: {}", dsl);
        return dsl;
    }

    public String analyzeData(String dataSummary, String userQuestion) {
        String content = "用户问题：" + userQuestion + "\n\n" + dataSummary;
        return callDeepSeek(content, analysisPrompt);
    }

    private String callDeepSeek(String userMessage, String systemMsg) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("stream", false);

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode systemNode = messages.addObject();
            systemNode.put("role", "system");
            systemNode.put("content", systemMsg);

            ObjectNode userNode = messages.addObject();
            userNode.put("role", "user");
            userNode.put("content", userMessage);

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                return responseJson.get("choices").get(0).get("message").get("content").asText().trim();
            } else {
                log.error("LLM API调用失败，状态码: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("LLM API调用异常: {}", e.getMessage());
            return null;
        }
    }
}
