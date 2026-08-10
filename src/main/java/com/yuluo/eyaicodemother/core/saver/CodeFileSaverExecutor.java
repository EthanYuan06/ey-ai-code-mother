package com.yuluo.eyaicodemother.core.saver;

import com.yuluo.eyaicodemother.ai.model.HtmlCodeResult;
import com.yuluo.eyaicodemother.ai.model.MultiFileCodeResult;
import com.yuluo.eyaicodemother.exception.BusinessException;
import com.yuluo.eyaicodemother.exception.ErrorCode;
import com.yuluo.eyaicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 代码文件保存执行器
 */
public class CodeFileSaverExecutor {

    // 代码保存器不依赖实例字段，是无状态的，全局只需要一个单例
    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaver =
            new HtmlCodeFileSaverTemplate();

    private static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaver =
            new MultiFileCodeFileSaverTemplate();

    /**
     * 执行代码保存
     *
     * @param codeResult 代码结果
     * @param codeGenType 代码生成类型
     * @param appId 应用 ID
     * @return 保存的文件
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType, Long appId) {
        return switch (codeGenType) {
            case HTML -> htmlCodeFileSaver.saveCode((HtmlCodeResult) codeResult, appId);
            case MULTI_FILE -> multiFileCodeFileSaver.saveCode((MultiFileCodeResult) codeResult, appId);
            default -> throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType);
        };
    }
}
