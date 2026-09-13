package com.yuluo.eyaicodemother.core.saver;

import cn.hutool.core.util.StrUtil;
import com.yuluo.eyaicodemother.ai.model.HtmlCodeResult;
import com.yuluo.eyaicodemother.exception.BusinessException;
import com.yuluo.eyaicodemother.exception.ErrorCode;
import com.yuluo.eyaicodemother.model.enums.CodeGenTypeEnum;

/**
 * 单文件HTML代码保存器
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult>{
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        if (StrUtil.isBlank(result.getHtmlCode())){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码不能为空");
        }
    }
}
