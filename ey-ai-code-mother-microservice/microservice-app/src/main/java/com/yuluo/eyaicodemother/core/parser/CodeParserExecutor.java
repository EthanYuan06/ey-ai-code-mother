package com.yuluo.eyaicodemother.core.parser;

import com.yuluo.eyaicodemother.ai.model.HtmlCodeResult;
import com.yuluo.eyaicodemother.ai.model.MultiFileCodeResult;
import com.yuluo.eyaicodemother.exception.BusinessException;
import com.yuluo.eyaicodemother.exception.ErrorCode;
import com.yuluo.eyaicodemother.model.enums.CodeGenTypeEnum;

/**
 * 代码解析执行器
 * 根据代码生成类型，执行相应的解析逻辑
 */
public class CodeParserExecutor {

    private static final CodeParser<HtmlCodeResult> HTML_CODE_PARSER = new HtmlCodeParser();

    private static final CodeParser<MultiFileCodeResult> MULTI_FILE_CODE_PARSER = new MultiFileCodeParser();

    /**
     * 执行代码解析
     *
     * @param codeContent      代码内容
     * @param codeGenType  生成类型
     * @return 解析结果
     */
    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenType){
        return switch (codeGenType) {
            case HTML -> HTML_CODE_PARSER.parseCode(codeContent);
            case MULTI_FILE -> MULTI_FILE_CODE_PARSER.parseCode(codeContent);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的生成类型："
                    + codeGenType.getValue());
        };
    }
}
