package com.loganalytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Service
public class DslValidator {

    private static final Logger log = LoggerFactory.getLogger(DslValidator.class);

    private static final Set<String> KEYWORD_FIELDS = new HashSet<>();
    static {
        KEYWORD_FIELDS.add("page");
        KEYWORD_FIELDS.add("event");
        KEYWORD_FIELDS.add("device");
        KEYWORD_FIELDS.add("user_id");
    }

    private final ObjectMapper objectMapper;

    public DslValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String validate(String rawDsl) {
        if (rawDsl == null || rawDsl.trim().isEmpty()) {
            log.warn("DSL为空");
            return null;
        }

        String cleanDsl = rawDsl.trim();
        if (cleanDsl.startsWith("```")) {
            int start = cleanDsl.indexOf('\n');
            int end = cleanDsl.lastIndexOf("```");
            if (start > 0 && end > start) {
                cleanDsl = cleanDsl.substring(start, end).trim();
            }
        }

        try {
            JsonNode dslJson = objectMapper.readTree(cleanDsl);

            if (containsDangerousOperation(dslJson)) {
                log.warn("DSL包含危险操作");
                return null;
            }

            if (dslJson.has("size") && dslJson.get("size").asInt() > 100) {
                ((ObjectNode) dslJson).put("size", 100);
            }

            fixKeywordFields(dslJson);
            fixTextMatchQueries(dslJson);

            return objectMapper.writeValueAsString(dslJson);
        } catch (JsonProcessingException e) {
            log.warn("DSL格式非法，不是有效的JSON: {}", e.getMessage());
            return null;
        }
    }

    private void fixTextMatchQueries(JsonNode node) {
        if (node == null) return;

        if (node.isObject()) {
            if (node.has("match")) {
                JsonNode matchNode = node.get("match");
                if (matchNode.isObject()) {
                    Iterator<String> fields = matchNode.fieldNames();
                    while (fields.hasNext()) {
                        String fieldName = fields.next();
                        if ("error_msg".equals(fieldName)) {
                            JsonNode value = matchNode.get(fieldName);
                            ((ObjectNode) node).remove("match");
                            ObjectNode termNode = ((ObjectNode) node).putObject("term");
                            termNode.set("error_msg.keyword", value);
                            log.info("自动修复match查询: error_msg -> error_msg.keyword (term查询)");
                        }
                    }
                }
            } else {
                Iterator<String> fieldNames = node.fieldNames();
                while (fieldNames.hasNext()) {
                    fixTextMatchQueries(node.get(fieldNames.next()));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                fixTextMatchQueries(element);
            }
        }
    }

    private void fixKeywordFields(JsonNode node) {
        if (node == null) return;

        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if ("field".equals(fieldName) && node.get(fieldName).isTextual()) {
                    String fieldValue = node.get(fieldName).asText();
                    String stripped = fieldValue.endsWith(".keyword")
                            ? fieldValue.substring(0, fieldValue.length() - 8)
                            : fieldValue;
                    if (KEYWORD_FIELDS.contains(stripped) && !fieldValue.equals(stripped)) {
                        ((ObjectNode) node).put("field", stripped);
                        log.info("自动修复字段名: {} -> {}", fieldValue, stripped);
                    }
                } else {
                    fixKeywordFields(node.get(fieldName));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                fixKeywordFields(element);
            }
        }
    }

    private boolean containsDangerousOperation(JsonNode dsl) {
        if (dsl.isObject()) {
            Iterator<String> fieldNames = dsl.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next().toLowerCase();
                if (field.contains("delete") || field.contains("update") || field.contains("_bulk")) {
                    return true;
                }
            }
            Iterator<JsonNode> elements = dsl.elements();
            while (elements.hasNext()) {
                if (containsDangerousOperation(elements.next())) {
                    return true;
                }
            }
        }
        return false;
    }
}
