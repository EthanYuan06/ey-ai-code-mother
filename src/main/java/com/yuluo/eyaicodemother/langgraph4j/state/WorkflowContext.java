package com.yuluo.eyaicodemother.langgraph4j.state;

import com.yuluo.eyaicodemother.langgraph4j.model.ImageResource;
import com.yuluo.eyaicodemother.langgraph4j.model.QualityResult;
import com.yuluo.eyaicodemother.model.enums.CodeGenTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 工作流上下文 - 存储所有状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowContext implements Serializable {

    /**
     * WorkflowContext 在 MessagesState 中的存储key
     */
    public static final String WORKFLOW_CONTEXT_KEY = "workflowContext";

    /**
     * 当前执行步骤
     */
    private String currentStep;

    /**
     * 应用 ID（关联业务应用）
     */
    private Long appId;

    /**
     * 用户 ID（用于记录对话历史）
     */
    private Long userId;

    /**
     * 是否为多轮对话（非首次请求）
     */
    private boolean isMultiTurn;

    /**
     * 用户原始输入的提示词
     */
    private String originalPrompt;

    /**
     * 图片资源字符串
     */
    private String imageListStr;

    /**
     * 图片资源列表
     */
    private List<ImageResource> imageList;

    /**
     * 增强后的提示词
     */
    private String enhancedPrompt;

    /**
     * 代码生成类型
     */
    private CodeGenTypeEnum generationType;

    /**
     * 生成的代码目录
     */
    private String generatedCodeDir;

    /**
     * 生成的完整代码内容（用于透传给前端）
     */
    private String codeContent;

    /**
     * 质检重试次数
     */
    private int retryCount;

    /**
     * 最大重试次数
     */
    public static final int MAX_RETRY_COUNT = 2;

    /**
     * 构建成功的目录
     */
    private String buildResultDir;

    /**
     * 错误信息
     */
    private String errorMessage;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 质量检查结果
     */
    private QualityResult qualityResult;

    // ========== 上下文操作方法 ==========

    /**
     * 从 MessagesState 中获取 WorkflowContext
     */
    public static WorkflowContext getContext(MessagesState<String> state) {
        return (WorkflowContext) state.data().get(WORKFLOW_CONTEXT_KEY);
    }

    /**
     * 将 WorkflowContext 保存到 MessagesState 中
     */
    public static Map<String, Object> saveContext(WorkflowContext context) {
        return Map.of(WORKFLOW_CONTEXT_KEY, context);
    }
}
