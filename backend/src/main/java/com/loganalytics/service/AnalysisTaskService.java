package com.loganalytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalysisTaskService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTaskService.class);

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Lazy
    @Autowired
    private AnalysisTaskService self;

    private final ConcurrentHashMap<String, AnalysisTask> taskCache = new ConcurrentHashMap<>();

    private static final long TASK_EXPIRE_MS = 10 * 60 * 1000;

    public AnalysisTaskService(LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    public String submitAnalysis(JsonNode esResult, String question) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        AnalysisTask task = new AnalysisTask(taskId, question);
        taskCache.put(taskId, task);
        log.info("提交分析任务: taskId={}, question={}", taskId, question);
        self.doAnalyze(taskId, esResult, question);
        return taskId;
    }

    @Async("analysisExecutor")
    public void doAnalyze(String taskId, JsonNode esResult, String question) {
        AnalysisTask task = taskCache.get(taskId);
        if (task == null) {
            log.warn("分析任务不存在: taskId={}", taskId);
            return;
        }

        try {
            task.status = TaskStatus.RUNNING;

            long totalCount = 0;
            if (esResult.has("hits") && esResult.get("hits").has("total")) {
                JsonNode total = esResult.get("hits").get("total");
                totalCount = total.isObject() ? total.get("value").asLong() : total.asLong();
            }

            StringBuilder dataSummary = new StringBuilder();
            dataSummary.append("查询结果概要：\n");
            dataSummary.append("- 总记录数：").append(totalCount).append("条\n");

            JsonNode aggregations = esResult.get("aggregations");
            if (aggregations != null) {
                dataSummary.append("- 聚合数据：\n");
                Iterator<String> aggNames = aggregations.fieldNames();
                while (aggNames.hasNext()) {
                    String aggName = aggNames.next();
                    JsonNode agg = aggregations.get(aggName);
                    dataSummary.append("  【").append(aggName).append("】\n");
                    if (agg.has("buckets")) {
                        JsonNode buckets = agg.get("buckets");
                        for (JsonNode bucket : buckets) {
                            String key = bucket.has("key_as_string")
                                    ? bucket.get("key_as_string").asText()
                                    : bucket.get("key").asText();
                            long count = bucket.get("doc_count").asLong();
                            dataSummary.append("    - ").append(key).append(": ").append(count).append("次\n");
                        }
                    }
                }
            }

            String result = llmService.analyzeData(dataSummary.toString(), question);
            task.result = result;
            task.status = TaskStatus.SUCCESS;
            log.info("分析任务完成: taskId={}", taskId);

        } catch (Exception e) {
            log.error("分析任务执行异常: taskId={}, error={}", taskId, e.getMessage(), e);
            task.status = TaskStatus.FAILED;
            task.error = "AI分析失败: " + e.getMessage();
        }
    }

    public String getTaskStatus(String taskId) {
        AnalysisTask task = taskCache.get(taskId);
        if (task == null) return null;
        return task.status.name();
    }

    public AnalysisTask getTask(String taskId) {
        AnalysisTask task = taskCache.get(taskId);
        if (task == null) return null;
        if (System.currentTimeMillis() - task.createdAt > TASK_EXPIRE_MS) {
            taskCache.remove(taskId);
            return null;
        }
        return task;
    }

    public void cleanupByQuery(String question) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, AnalysisTask>> iter = taskCache.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, AnalysisTask> entry = iter.next();
            AnalysisTask task = entry.getValue();
            if (task.question.equals(question)) {
                iter.remove();
                log.info("清理旧分析任务: taskId={}, question={}", entry.getKey(), question);
            } else if (now - task.createdAt > TASK_EXPIRE_MS) {
                iter.remove();
                log.info("清理过期分析任务: taskId={}", entry.getKey());
            }
        }
    }

    public enum TaskStatus {
        PENDING, RUNNING, SUCCESS, FAILED
    }

    public static class AnalysisTask {
        private final String taskId;
        private final String question;
        private final long createdAt;
        private volatile TaskStatus status;
        private volatile String result;
        private volatile String error;

        public AnalysisTask(String taskId, String question) {
            this.taskId = taskId;
            this.question = question;
            this.createdAt = System.currentTimeMillis();
            this.status = TaskStatus.PENDING;
        }

        public String getTaskId() { return taskId; }
        public String getQuestion() { return question; }
        public long getCreatedAt() { return createdAt; }
        public TaskStatus getStatus() { return status; }
        public String getResult() { return result; }
        public String getError() { return error; }
    }
}
