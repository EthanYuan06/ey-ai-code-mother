package com.yuluo.eyaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yuluo.eyaicodemother.model.dto.app.AppAddRequest;
import com.yuluo.eyaicodemother.model.dto.app.AppQueryRequest;
import com.yuluo.eyaicodemother.model.entity.App;
import com.yuluo.eyaicodemother.model.entity.User;
import com.yuluo.eyaicodemother.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author EthanYuan
 */
public interface AppService extends IService<App> {

    /**
     * 创建应用
     *
     * @param appAddRequest 创建应用请求参数
     * @param loginUser 当前登录用户
     * @return 应用 ID
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 部署应用
     *
     * @param appId 应用 ID
     * @param loginUser 当前登录用户
     * @return 可访问的 URL
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 通过对话生成应用代码
     *
     * @param appId 应用 ID
     * @param message 用户消息
     * @param loginUser 当前登录用户
     * @return 流式响应
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 校验应用参数
     *
     * @param app 应用
     * @param add 是否为创建校验
     */
    void validApp(App app, boolean add);

    /**
     * 获取脱敏后的应用信息
     *
     * @param app 应用
     * @return 应用视图
     */
    AppVO getAppVO(App app);

    /**
     * 获取脱敏后的应用信息（分页）
     *
     * @param appList 应用列表
     * @return 应用视图列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据查询条件构造数据查询参数
     *
     * @param appQueryRequest 查询请求参数
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId 应用 ID
     * @param appDeployUrl 应用部署地址
     */
    void generateAppScreenshotAsync(Long appId, String appDeployUrl);
}
