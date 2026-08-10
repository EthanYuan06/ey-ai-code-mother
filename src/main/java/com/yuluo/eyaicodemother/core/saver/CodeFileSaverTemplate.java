package com.yuluo.eyaicodemother.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yuluo.eyaicodemother.exception.BusinessException;
import com.yuluo.eyaicodemother.exception.ErrorCode;
import com.yuluo.eyaicodemother.exception.ThrowUtils;
import com.yuluo.eyaicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码保存器模板类
 */
public abstract class CodeFileSaverTemplate<T> {

    // 文件保存根目录
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 模板方法：保存代码标准流程
     *
     * @param result 代码解析结果
     * @param appId 应用 ID
     * @return 保存文件
     */
    public final File saveCode(T result, Long appId){
        // 验证输入
        validateInput(result);
        // 构建保存路径
        String baseDirPath = buildUniqueDir(appId);
        // 保存文件
        saveFiles(result, baseDirPath);
        // 返回目录文件对象
        return new File(baseDirPath);
    }

    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码解析结果不能为空");
        }
    }

    /**
     * 写入单个文件（父类提供的通用方法，不允许子类重写）
     *
     * @param dirPath 目录路径
     * @param filename 文件名
     * @param content 文件内容
     */
    protected final void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isNotBlank(content)) {
            String filePath = dirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }

    /**
     * 构建唯一目录路径：tmp/code_output/appId
     */
    protected String buildUniqueDir(Long appId) {
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 获取代码生成类型（子类实现）
     *
     * @return 代码生成类型
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 保存文件（子类实现）
     * 
     * @param result 代码解析结果
     * @param baseDirPath 文件路径
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}























