package com.yuluo.eyaicodemother.langgraph4j.node;

import com.yuluo.eyaicodemother.ai.AiCodeGenTypeRoutingService;
import com.yuluo.eyaicodemother.langgraph4j.state.WorkflowContext;
import com.yuluo.eyaicodemother.model.enums.CodeGenTypeEnum;
import com.yuluo.eyaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 智能路由节点
 */
@Slf4j
public class RouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点：智能路由");
    
            CodeGenTypeEnum generationType;
                            
            // 多轮对话时，直接使用 context 中的代码生成类型（无需查库）
            if (context.isMultiTurn() && context.getGenerationType() != null) {
                generationType = context.getGenerationType();
                log.info("多轮对话，使用 context 中的类型：{} ({})", generationType.getValue(), generationType.getText());
            } else {
                try {
                    // 获取AI路由服务
                    AiCodeGenTypeRoutingService routingService = SpringContextUtil.getBean(AiCodeGenTypeRoutingService.class);
                    // 根据原始提示词进行智能路由
                    generationType = routingService.routeCodeGenType(context.getOriginalPrompt());
                    log.info("AI智能路由完成，选择类型: {} ({})", generationType.getValue(), generationType.getText());
                } catch (Exception e) {
                    log.error("AI智能路由失败，使用默认HTML类型: {}", e.getMessage());
                    generationType = CodeGenTypeEnum.HTML;
                }
            }
    
            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}

