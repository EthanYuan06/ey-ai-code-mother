package com.yuluo.eyaicodemother.controller;

import com.yuluo.eyaicodemother.langgraph4j.CodeGenWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 工作流 SSE 控制器
 * 演示 LangGraph4j 工作流的流式输出功能
 */
@RestController
@RequestMapping("/workflow")
@Slf4j
@Tag(name = "工作流接口")
public class WorkflowSseController {

    /**
     * Flux 流式执行工作流
     */
    @Operation(summary = "Flux 流式执行工作流")
    @GetMapping(value = "/execute-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeWorkflowWithFlux(@RequestParam String prompt) {
        log.info("收到 Flux 工作流执行请求: {}", prompt);
        return new CodeGenWorkflow().executeWorkflowWithFlux(prompt);
    }
}
