package com.loganalytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class AnalysisTemplateService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTemplateService.class);

    private final ObjectMapper objectMapper;

    @Value("${llm.prompt.analysis-templates}")
    private String analysisTemplates;

    public AnalysisTemplateService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generateSuggestion(JsonNode esResponse, String userQuestion) {
        try {
            long totalCount = 0;
            if (esResponse.has("hits") && esResponse.get("hits").has("total")) {
                JsonNode total = esResponse.get("hits").get("total");
                totalCount = total.isObject() ? total.get("value").asLong() : total.asLong();
            }

            JsonNode aggregations = esResponse.get("aggregations");
            if (aggregations == null) {
                return String.format("查询完成，共找到 %d 条相关记录。", totalCount);
            }

            String timeRange = extractTimeRange(userQuestion);
            return buildSuggestion(aggregations, userQuestion, timeRange, totalCount);
        } catch (Exception e) {
            log.warn("生成分析建议时出错: {}", e.getMessage());
            return "查询已完成，请查看下方数据详情。";
        }
    }

    private String buildSuggestion(JsonNode aggregations, String question, String timeRange, long totalCount) {
        String firstAggName = aggregations.fieldNames().next();
        JsonNode firstAgg = aggregations.get(firstAggName);

        if (!firstAgg.has("buckets")) {
            return String.format("在%s内，共找到 %d 条记录。", timeRange, totalCount);
        }

        JsonNode buckets = firstAgg.get("buckets");
        if (!buckets.isArray() || buckets.size() == 0) {
            return String.format("在%s内，共找到 %d 条记录。", timeRange, totalCount);
        }

        boolean isDateHistogram = isDateHistogramBucket(buckets.get(0));
        boolean isErrorQuery = question.contains("error") || question.contains("错误") || question.contains("失败");
        boolean isPageQuery = question.contains("页面") || question.contains("page") || question.contains("访问");
        boolean isEventQuery = question.contains("事件") || question.contains("event") || question.contains("分布");
        boolean isDeviceQuery = question.contains("设备") || question.contains("device");
        boolean isTrendQuery = question.contains("趋势") || question.contains("走势");

        if (isDateHistogram) {
            return buildTrendSuggestion(buckets, timeRange, totalCount);
        } else if (isErrorQuery) {
            return buildCategorySuggestion(buckets, timeRange, totalCount, "错误类型", "建议优先处理排名靠前的错误类型。");
        } else if (isPageQuery || isTrendQuery) {
            return buildCategorySuggestion(buckets, timeRange, totalCount, "热门页面", "");
        } else if (isDeviceQuery) {
            return buildCategorySuggestion(buckets, timeRange, totalCount, "设备类型", "");
        } else if (isEventQuery) {
            return buildCategorySuggestion(buckets, timeRange, totalCount, "事件类型", "");
        } else {
            return buildCategorySuggestion(buckets, timeRange, totalCount, "分类", "");
        }
    }

    private boolean isDateHistogramBucket(JsonNode bucket) {
        if (bucket.has("key_as_string")) {
            return true;
        }
        if (bucket.has("key")) {
            String key = bucket.get("key").asText();
            return key.matches("\\d{13}") || key.matches("\\d{10}");
        }
        return false;
    }

    private String buildTrendSuggestion(JsonNode buckets, String timeRange, long totalCount) {
        long maxCount = 0;
        String peakTime = "";
        for (JsonNode bucket : buckets) {
            long count = bucket.get("doc_count").asLong();
            if (count > maxCount) {
                maxCount = count;
                peakTime = formatBucketKey(bucket);
            }
        }
        return String.format("在%s内，共记录%d条事件，数据呈现%s趋势。访问高峰出现在%s，达到%d次。",
                timeRange, totalCount,
                isIncreasing(buckets) ? "上升" : "波动",
                peakTime, maxCount);
    }

    private boolean isIncreasing(JsonNode buckets) {
        if (buckets.size() < 3) return false;
        long first = buckets.get(0).get("doc_count").asLong();
        long last = buckets.get(buckets.size() - 1).get("doc_count").asLong();
        return last > first * 1.2;
    }

    private String buildCategorySuggestion(JsonNode buckets, String timeRange, long totalCount, String categoryName, String suffix) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("在%s内，共记录%d条数据。", timeRange, totalCount));

        sb.append(String.format(" 各%s分布为：", categoryName));
        List<String> itemList = new ArrayList<>();
        for (int i = 0; i < Math.min(buckets.size(), 5); i++) {
            JsonNode bucket = buckets.get(i);
            String key = formatBucketKey(bucket);
            long count = bucket.get("doc_count").asLong();
            double pct = totalCount > 0 ? (count * 100.0 / totalCount) : 0;
            itemList.add(String.format("%s（%d次, %.1f%%）", key, count, pct));
        }
        sb.append(String.join("、", itemList));
        sb.append("。");
        if (!suffix.isEmpty()) {
            sb.append(suffix);
        }
        return sb.toString();
    }

    private String formatBucketKey(JsonNode bucket) {
        if (bucket.has("key_as_string")) {
            String raw = bucket.get("key_as_string").asText();
            try {
                if (raw.length() >= 16) {
                    return raw.substring(0, 16).replace("T", " ");
                }
            } catch (Exception ignored) {}
            return raw;
        }
        String key = bucket.get("key").asText();
        if (key.matches("\\d{13}")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
                Date date = new Date(Long.parseLong(key));
                return sdf.format(date);
            } catch (Exception ignored) {}
        }
        return key;
    }

    private String extractTimeRange(String question) {
        if (question.contains("最近一小时") || question.contains("1小时") || question.contains("近1小时")) {
            return "最近1小时";
        } else if (question.contains("最近24小时") || question.contains("24小时") || question.contains("今天")) {
            return "最近24小时";
        } else if (question.contains("最近7天") || question.contains("7天") || question.contains("一周")) {
            return "最近7天";
        } else if (question.contains("最近30天") || question.contains("30天") || question.contains("一个月")) {
            return "最近30天";
        }
        return "查询时间段";
    }
}
