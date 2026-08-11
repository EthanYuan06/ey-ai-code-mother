package com.yuluo.eyaicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用视图
 */
@Data
public class AppVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 初始化提示词
     */
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     */
    private String codeGenType;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 部署时间
     */
    private LocalDateTime deployedTime;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建用户信息
     */
    private UserVO userVO;

    /**
     * 编辑时间
     */
    private LocalDateTime editTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
