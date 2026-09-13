package com.yuluo.eyaicodemother.service;

public interface ScreenshotService {
    /**
     * 生成并上传截图
     *
     * @param webUrl 网页URL
     * @return 可访问URL
     */
    String generateAndUploadScreenshot(String webUrl);
}
