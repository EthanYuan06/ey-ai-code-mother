package com.yuluo.eyaicodemother.constant;

/**
 * 应用常量
 */
public interface AppConstant {

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署域名默认值（本地开发使用）。
     * 生产 / Docker 环境应通过配置项 app.deploy.host 与 app.deploy.screenshot-host 覆盖，
     * 以便区分「返回给用户浏览器的公网访问 URL」与「容器内 headless Chrome 截图使用的内部 URL」。
     */
    String CODE_DEPLOY_HOST = "http://localhost";
}
