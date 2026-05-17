package com.loganalytics.controller;

import com.loganalytics.model.QueryRequest;
import com.loganalytics.model.QueryResponse;
import com.loganalytics.service.AnalysisTaskService;
import com.loganalytics.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {

    private static final Logger log = LoggerFactory.getLogger(LogController.class);

    private final LogService logService;
    private final AnalysisTaskService analysisTaskService;

    public LogController(LogService logService, AnalysisTaskService analysisTaskService) {
        this.logService = logService;
        this.analysisTaskService = analysisTaskService;
    }

    @PostMapping("/search")
    public QueryResponse search(@Valid @RequestBody QueryRequest request) {
        log.info("收到查询请求: question={}, page={}, size={}",
                request.getQuestion(), request.getPage(), request.getSize());
        return logService.search(request.getQuestion(), request.getPage(), request.getSize());
    }

    @GetMapping("/analyze/{taskId}")
    public QueryResponse getAnalysisResult(@PathVariable String taskId) {
        AnalysisTaskService.AnalysisTask task = analysisTaskService.getTask(taskId);
        if (task == null) {
            return QueryResponse.error(404, "分析任务不存在或已过期");
        }
        QueryResponse.Data data = new QueryResponse.Data();
        data.setAnalysisTaskId(taskId);
        data.setAnalysisTask(QueryResponse.AnalysisTaskResult.fromTask(task));
        return QueryResponse.success(data);
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
