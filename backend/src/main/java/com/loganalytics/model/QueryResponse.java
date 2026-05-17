package com.loganalytics.model;

import com.loganalytics.service.AnalysisTaskService;

import java.util.List;
import java.util.Map;

public class QueryResponse {

    private int code;
    private String message;
    private Data data;

    public static QueryResponse success(Data data) {
        QueryResponse response = new QueryResponse();
        response.code = 200;
        response.message = "success";
        response.data = data;
        return response;
    }

    public static QueryResponse error(int code, String message) {
        QueryResponse response = new QueryResponse();
        response.code = code;
        response.message = message;
        return response;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        private Map<String, Object> aggregations;
        private List<Map<String, Object>> logs;
        private Pagination pagination;
        private String analysisTaskId;
        private AnalysisTaskResult analysisTask;

        public Map<String, Object> getAggregations() {
            return aggregations;
        }

        public void setAggregations(Map<String, Object> aggregations) {
            this.aggregations = aggregations;
        }

        public List<Map<String, Object>> getLogs() {
            return logs;
        }

        public void setLogs(List<Map<String, Object>> logs) {
            this.logs = logs;
        }

        public Pagination getPagination() {
            return pagination;
        }

        public void setPagination(Pagination pagination) {
            this.pagination = pagination;
        }

        public String getAnalysisTaskId() {
            return analysisTaskId;
        }

        public void setAnalysisTaskId(String analysisTaskId) {
            this.analysisTaskId = analysisTaskId;
        }

        public AnalysisTaskResult getAnalysisTask() {
            return analysisTask;
        }

        public void setAnalysisTask(AnalysisTaskResult analysisTask) {
            this.analysisTask = analysisTask;
        }
    }

    public static class Pagination {

        private int page;
        private int size;
        private long total;

        public Pagination(int page, int size, long total) {
            this.page = page;
            this.size = size;
            this.total = total;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }
    }

    public static class AnalysisTaskResult {
        private String taskId;
        private String status;
        private String result;
        private String error;

        public static AnalysisTaskResult pending(String taskId) {
            AnalysisTaskResult r = new AnalysisTaskResult();
            r.taskId = taskId;
            r.status = "PENDING";
            return r;
        }

        public static AnalysisTaskResult fromTask(AnalysisTaskService.AnalysisTask task) {
            AnalysisTaskResult r = new AnalysisTaskResult();
            r.taskId = task.getTaskId();
            r.status = task.getStatus().name();
            r.result = task.getResult();
            r.error = task.getError();
            return r;
        }

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
