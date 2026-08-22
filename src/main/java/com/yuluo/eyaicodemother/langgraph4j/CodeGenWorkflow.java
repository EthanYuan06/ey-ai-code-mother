package com.yuluo.eyaicodemother.langgraph4j;

import cn.hutool.json.JSONUtil;
import com.yuluo.eyaicodemother.exception.BusinessException;
import com.yuluo.eyaicodemother.exception.ErrorCode;
import com.yuluo.eyaicodemother.langgraph4j.model.QualityResult;
import com.yuluo.eyaicodemother.langgraph4j.node.*;
import com.yuluo.eyaicodemother.langgraph4j.state.WorkflowContext;
import com.yuluo.eyaicodemother.model.enums.CodeGenTypeEnum;
import com.yuluo.eyaicodemother.model.entity.App;
import com.yuluo.eyaicodemother.service.AppService;
import com.yuluo.eyaicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 代码生成工作流（Spring Bean）
 * 整合图片收集、提示词增强、智能路由、代码生成、质量检查等能力
 */
@Service
@Slf4j
public class CodeGenWorkflow {

    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    @Lazy
    private AppService appService;

    /**
     * 创建完整的工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 添加节点 - 使用完整实现的节点
                    .addNode("image_collector", ImageCollectorNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())

                    // 添加边
                    .addEdge(START, "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    "build", "project_builder", // 需要构建就走构建节点
                                    "skip_build", END, // 不需要构建就跳过直接结束
                                    "fail", "code_generator"
                            )
                    )
                    .addEdge("project_builder", END)

                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流创建失败");
        }
    }

    /**
     * 执行工作流（Flux 流式输出）- 业务整合入口
     *
     * @param originalPrompt 用户原始提示词
     * @param appId          应用 ID
     * @param userId         用户 ID
     * @return SSE 流式响应
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt, Long appId, Long userId) {
        return Flux.create(sink -> {
            Thread.startVirtualThread(() -> {
                try {
                    // 加载对话历史，判断是否为多轮对话
                    int historyCount = chatHistoryService.countByAppId(appId);
                    boolean isMultiTurn = historyCount > 0;
                    log.info("appId: {}, 对话历史数量：{}, 是否多轮对话：{}", appId, historyCount, isMultiTurn);
                                    
                    // 查询代码生成类型（仅查一次，存入 context 供后续节点使用）
                    CodeGenTypeEnum codeGenType = loadCodeGenType(appId);
                    log.info("appId: {}, 代码生成类型：{}", appId, codeGenType);
                                    
                    CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                    WorkflowContext initialContext = WorkflowContext.builder()
                            .originalPrompt(originalPrompt)
                            .appId(appId)
                            .userId(userId)
                            .isMultiTurn(isMultiTurn)
                            .generationType(codeGenType)
                            .currentStep("初始化")
                            .build();
                    sink.next(formatSseEvent("workflow_start", Map.of(
                            "message", "开始执行代码生成工作流",
                            "originalPrompt", originalPrompt
                    )));
                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("工作流图:\n{}", graph.content());

                    int stepCounter = 1;
                    String lastCompletedNode = null;
                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                        log.info("--- 第 {} 步完成 ---", stepCounter);
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            sink.next(formatSseEvent("step_completed", Map.of(
                                    "stepNumber", stepCounter,
                                    "currentStep", currentContext.getCurrentStep()
                            )));
                            log.info("当前步骤上下文: {}", currentContext);
                            // 代码生成步骤完成后，发射代码内容给前端
                            if ("代码生成".equals(currentContext.getCurrentStep())
                                    && currentContext.getCodeContent() != null
                                    && !currentContext.getCodeContent().isEmpty()) {
                                String codeContent = currentContext.getCodeContent();
                                // 发射代码内容（前端可识别的格式）
                                sink.next(JSONUtil.toJsonStr(Map.of("d", codeContent)));
                                log.info("已发射代码内容，长度: {} 字符", codeContent.length());
                            }
                        }
                        stepCounter++;
                    }
                    sink.next(formatSseEvent("workflow_completed", Map.of(
                            "message", "代码生成工作流执行完成！"
                    )));
                    log.info("代码生成工作流执行完成！");
                    sink.complete();
                } catch (Exception e) {
                    log.error("工作流执行失败: {}", e.getMessage(), e);
                    sink.next(formatSseEvent("workflow_error", Map.of(
                            "error", e.getMessage(),
                            "message", "工作流执行失败"
                    )));
                    sink.error(e);
                }
            });
        });
    }

    /**
     * 执行工作流（单元测试使用）
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();

        // 初始化 WorkflowContext
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("初始化")
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", graph.content());
        log.info("开始执行代码生成工作流");

        WorkflowContext finalContext = null;
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            // 显示当前状态
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("当前步骤上下文: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("代码生成工作流执行完成！");
        return finalContext;
    }

    /**
     * 格式化 SSE 事件的辅助方法
     * 解决浏览器解析流式内容丢失空格的问题
     */
    private String formatSseEvent(String eventType, Object data) {
        try {
            String jsonData = JSONUtil.toJsonStr(data);
            return "event: " + eventType + "\ndata: " + jsonData + "\n\n";
        } catch (Exception e) {
            log.error("格式化 SSE 事件失败: {}", e.getMessage(), e);
            return "event: error\ndata: {\"error\":\"格式化失败\"}\n\n";
        }
    }

    /**
     * 根据生成类型决定是否构建
     */
    private String routeBuildOrSkip(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();
        // HTML 和 MULTI_FILE 类型不需要构建，直接结束
        if (generationType == CodeGenTypeEnum.HTML || generationType == CodeGenTypeEnum.MULTI_FILE) {
            return "skip_build";
        }
        // VUE_PROJECT 需要构建
        return "build";
    }

    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();
        // 如果质检失败，检查重试次数
        if (qualityResult == null || !qualityResult.getIsValid()) {
            int retryCount = context.getRetryCount();
            if (retryCount >= WorkflowContext.MAX_RETRY_COUNT) {
                log.warn("质检重试次数已达上限({})，跳过重试继续流程", WorkflowContext.MAX_RETRY_COUNT);
                return routeBuildOrSkip(state);
            }
            context.setRetryCount(retryCount + 1);
            log.error("代码质检失败，第 {} 次重试", retryCount + 1);
            return "fail";
        }
        // 质检通过，使用原有的构建路由逻辑
        log.info("代码质检通过，继续后续流程");
        return routeBuildOrSkip(state);
    }

    /**
     * 从数据库加载代码生成类型（仅查一次）
     */
    private CodeGenTypeEnum loadCodeGenType(Long appId) {
        try {
            App app = appService.getById(appId);
            if (app != null && app.getCodeGenType() != null) {
                return CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
            }
        } catch (Exception e) {
            log.error("加载代码生成类型失败，appId: {}", appId, e);
        }
        return null;
    }

}
