package com.loganalytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loganalytics.model.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LlmService llmService;
    private final DslValidator dslValidator;
    private final AnalysisTaskService analysisTaskService;

    @Value("${elasticsearch.scheme}")
    private String esScheme;

    @Value("${elasticsearch.host}")
    private String esHost;

    @Value("${elasticsearch.port}")
    private int esPort;

    @Value("${elasticsearch.index}")
    private String esIndex;

    public LogService(RestTemplate restTemplate, ObjectMapper objectMapper,
                      LlmService llmService, DslValidator dslValidator,
                      AnalysisTaskService analysisTaskService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.llmService = llmService;
        this.dslValidator = dslValidator;
        this.analysisTaskService = analysisTaskService;
    }

    public QueryResponse search(String question, int page, int size) {
        try {
            String rawDsl = llmService.generateDsl(question);
            if (rawDsl == null) {
                return QueryResponse.error(500, "LLM服务调用失败，请稍后重试");
            }

            String validatedDsl = dslValidator.validate(rawDsl);
            if (validatedDsl == null) {
                return QueryResponse.error(400, "无法理解您的问题，请换个方式描述");
            }

            String finalDsl = injectPagination(validatedDsl, page, size);
            log.info("最终执行的DSL: {}", finalDsl);

            JsonNode esResult = executeEsQuery(finalDsl);
            if (esResult == null) {
                return QueryResponse.error(500, "日志查询服务异常");
            }

            QueryResponse response = buildResponse(esResult, question, page, size);

            try {
                analysisTaskService.cleanupByQuery(question);
                String taskId = analysisTaskService.submitAnalysis(esResult, question);
                if (taskId != null) {
                    response.getData().setAnalysisTaskId(taskId);
                }
            } catch (Exception e) {
                log.warn("提交异步分析任务失败，不影响搜索: {}", e.getMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("查询处理异常: {}", e.getMessage(), e);
            return QueryResponse.error(500, "系统内部错误: " + e.getMessage());
        }
    }

    private String injectPagination(String dsl, int page, int size) {
        try {
            JsonNode dslJson = objectMapper.readTree(dsl);
            ObjectNode dslObject = (ObjectNode) dslJson;

            int from = (page - 1) * size;
            dslObject.put("from", from);
            dslObject.put("size", size);

            return objectMapper.writeValueAsString(dslObject);
        } catch (JsonProcessingException e) {
            log.warn("注入分页参数失败，使用原始DSL: {}", e.getMessage());
            return dsl;
        }
    }

    private JsonNode executeEsQuery(String dsl) {
        try {
            String url = String.format("%s://%s:%d/%s/_search", esScheme, esHost, esPort, esIndex);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(dsl, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readTree(response.getBody());
            } else {
                log.error("ES查询失败，状态码: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("ES查询异常: {}", e.getMessage());
            return null;
        }
    }

    public String submitAnalysis(String question, int page, int size) {
        try {
            String rawDsl = llmService.generateDsl(question);
            if (rawDsl == null) return null;

            String validatedDsl = dslValidator.validate(rawDsl);
            if (validatedDsl == null) return null;

            String finalDsl = injectPagination(validatedDsl, page, size);

            JsonNode esResult = executeEsQuery(finalDsl);
            if (esResult == null) return null;

            analysisTaskService.cleanupByQuery(question);
            return analysisTaskService.submitAnalysis(esResult, question);
        } catch (Exception e) {
            log.error("提交分析任务异常: {}", e.getMessage(), e);
            return null;
        }
    }

    private QueryResponse buildResponse(JsonNode esResult, String question, int page, int size) {
        QueryResponse.Data data = new QueryResponse.Data();

        long total = extractTotal(esResult);
        data.setPagination(new QueryResponse.Pagination(page, size, total));

        data.setLogs(extractLogs(esResult));

        data.setAggregations(extractAggregations(esResult));

        return QueryResponse.success(data);
    }

    private long extractTotal(JsonNode esResult) {
        try {
            JsonNode total = esResult.get("hits").get("total");
            if (total.isObject()) {
                return total.get("value").asLong();
            }
            return total.asLong();
        } catch (Exception e) {
            return 0;
        }
    }

    private List<Map<String, Object>> extractLogs(JsonNode esResult) {
        List<Map<String, Object>> logs = new ArrayList<>();
        try {
            JsonNode hits = esResult.get("hits").get("hits");
            if (hits.isArray()) {
                for (JsonNode hit : hits) {
                    Map<String, Object> logEntry = new LinkedHashMap<>();
                    JsonNode source = hit.get("_source");
                    if (source != null) {
                        source.fieldNames().forEachRemaining(field -> {
                            JsonNode value = source.get(field);
                            if (value.isTextual()) {
                                logEntry.put(field, value.asText());
                            } else if (value.isNumber()) {
                                logEntry.put(field, value.asLong());
                            } else {
                                logEntry.put(field, value.asText());
                            }
                        });
                    }
                    logs.add(logEntry);
                }
            }
        } catch (Exception e) {
            log.warn("提取日志明细失败: {}", e.getMessage());
        }
        return logs;
    }

    private Map<String, Object> extractAggregations(JsonNode esResult) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            JsonNode aggregations = esResult.get("aggregations");
            if (aggregations == null) {
                return null;
            }

            aggregations.fieldNames().forEachRemaining(aggName -> {
                JsonNode agg = aggregations.get(aggName);
                if (agg.has("buckets")) {
                    List<Map<String, Object>> buckets = new ArrayList<>();
                    for (JsonNode bucket : agg.get("buckets")) {
                        Map<String, Object> bucketMap = new LinkedHashMap<>();
                        String key = bucket.has("key_as_string")
                                ? bucket.get("key_as_string").asText()
                                : bucket.get("key").asText();
                        bucketMap.put("key", key);
                        bucketMap.put("count", bucket.get("doc_count").asLong());
                        buckets.add(bucketMap);
                    }
                    result.put(aggName, buckets);
                } else if (agg.has("buckets_array")) {
                    // date_histogram type
                    List<Map<String, Object>> buckets = new ArrayList<>();
                    for (JsonNode bucket : agg.get("buckets_array")) {
                        Map<String, Object> bucketMap = new LinkedHashMap<>();
                        bucketMap.put("key", bucket.get("key_as_string").asText());
                        bucketMap.put("count", bucket.get("doc_count").asLong());
                        buckets.add(bucketMap);
                    }
                    result.put(aggName, buckets);
                }
            });
        } catch (Exception e) {
            log.warn("提取聚合数据失败: {}", e.getMessage());
        }
        return result.isEmpty() ? null : result;
    }
}
