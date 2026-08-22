package com.yuluo.eyaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yuluo.eyaicodemother.ai.AiCodeGenTypeRoutingService;
import com.yuluo.eyaicodemother.constant.AppConstant;
import com.yuluo.eyaicodemother.core.AiCodeGeneratorFacade;
import com.yuluo.eyaicodemother.core.bulider.VueProjectBuilder;
import com.yuluo.eyaicodemother.core.handler.StreamHandlerExecutor;
import com.yuluo.eyaicodemother.exception.BusinessException;
import com.yuluo.eyaicodemother.exception.ErrorCode;
import com.yuluo.eyaicodemother.exception.ThrowUtils;
import com.yuluo.eyaicodemother.langgraph4j.CodeGenWorkflow;
import com.yuluo.eyaicodemother.mapper.AppMapper;
import com.yuluo.eyaicodemother.model.dto.app.AppAddRequest;
import com.yuluo.eyaicodemother.model.dto.app.AppQueryRequest;
import com.yuluo.eyaicodemother.model.entity.App;
import com.yuluo.eyaicodemother.model.entity.User;
import com.yuluo.eyaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.yuluo.eyaicodemother.model.enums.CodeGenTypeEnum;
import com.yuluo.eyaicodemother.model.vo.AppVO;
import com.yuluo.eyaicodemother.model.vo.UserVO;
import com.yuluo.eyaicodemother.service.AppService;
import com.yuluo.eyaicodemother.service.ChatHistoryService;
import com.yuluo.eyaicodemother.service.ScreenshotService;
import com.yuluo.eyaicodemother.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author EthanYuan
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
    @Resource
    private VueProjectBuilder vueProjectBuilder;
    @Resource
    private ScreenshotService screenshotService;
    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;
    @Resource
    private CodeGenWorkflow codeGenWorkflow;

    /**
     * 工作流模式开关：true=使用工作流生成代码（默认），false=使用原模式生成代码
     */
    @Value("${app.code-gen.workflow-enabled:true}")
    private boolean workflowEnabled;

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化提示词不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 暂时设置应用名称为初始化提示词的前12个字符
        app.setAppName(appAddRequest.getInitPrompt().substring(0, Math.min(appAddRequest.getInitPrompt().length(), 12)));
        // 使用AI代码生成类型智能路由
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 参数校验
        this.validApp(app, true);
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return app.getId();
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 权限校验：只有创建用户本人才能生成代码
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限生成代码");
        // 获取代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的代码生成类型");
        // 通过校验后，将用户消息添加到对话历史
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 根据配置选择生成模式：工作流模式（主）或原模式（兆底）
        if (workflowEnabled) {
            log.info("使用工作流模式生成代码，appId: {}", appId);
            return chatToGenCodeByWorkflow(appId, message, loginUser);
        }
        // 原模式：调用 AI 生成代码
        log.info("使用原模式生成代码，appId: {}", appId);
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 收集 AI 响应内容，并在完成后添加到对话历史（流处理执行器）
        // 因为响应数据有字符串也有JSON，使用流处理器分别处理
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum);
    }

    /**
     * 通过工作流生成代码（工作流模式）
     * 整合图片收集、提示词增强、智能路由、代码生成、质量检查等能力
     *
     * @param appId    应用 ID
     * @param message  用户消息
     * @param loginUser 登录用户
     * @return 流式响应
     */
    private Flux<String> chatToGenCodeByWorkflow(Long appId, String message, User loginUser) {
        // 调用工作流执行代码生成
        Flux<String> workflowFlux = codeGenWorkflow.executeWorkflowWithFlux(message, appId, loginUser.getId());
        // 收集 AI 响应内容（用于记录对话历史）
        StringBuilder aiResponseBuilder = new StringBuilder();
        // 将工作流的流式输出适配为前端统一的 SSE 格式
        return workflowFlux
                .<String>handle((sseEvent, sink) -> {
                    // 解析工作流 SSE 事件，提取内容并转换为统一格式
                    String content = extractEventContent(sseEvent);
                    if (content != null && !content.isEmpty()) {
                        aiResponseBuilder.append(content);
                        sink.next(JSONUtil.toJsonStr(Map.of("d", content)));
                    }
                })
                .doOnComplete(() -> {
                    // 工作流完成后，记录 AI 响应到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    if (!aiResponse.isEmpty()) {
                        try {
                            chatHistoryService.addChatMessage(
                                    appId,
                                    aiResponse,
                                    ChatHistoryMessageTypeEnum.AI.getValue(),
                                    loginUser.getId()
                            );
                            log.info("工作流模式：AI 响应已记录到对话历史，appId: {}", appId);
                        } catch (Exception e) {
                            log.error("记录 AI 响应到对话历史失败: {}", e.getMessage());
                        }
                    }
                });
    }

    /**
     * 从工作流 SSE 事件中提取内容
     * 支持两种格式：
     * 1. 工作流步骤事件: event: xxx\ndata: {json}\n\n
     * 2. 代码内容事件: {"d": "code content"}（已由工作流直接发射）
     */
    private String extractEventContent(String sseEvent) {
        // 工作流 SSE 格式: event: xxx\ndata: {json}\n\n
        if (sseEvent == null || sseEvent.isEmpty()) {
            return null;
        }
        try {
            // 先检查是否已经是前端格式的代码内容事件（{"d": "..."}）
            if (sseEvent.trim().startsWith("{")) {
                cn.hutool.json.JSONObject jsonObj = JSONUtil.parseObj(sseEvent);
                if (jsonObj.containsKey("d")) {
                    return jsonObj.getStr("d");
                }
            }
            // 解析工作流步骤事件
            String[] lines = sseEvent.split("\n");
            for (String line : lines) {
                if (line.startsWith("data: ")) {
                    String jsonStr = line.substring(6);
                    cn.hutool.json.JSONObject jsonObj = JSONUtil.parseObj(jsonStr);
                    // 根据不同事件类型提取内容
                    if (jsonObj.containsKey("message")) {
                        return jsonObj.getStr("message");
                    }
                    if (jsonObj.containsKey("currentStep")) {
                        return "[" + jsonObj.getStr("currentStep") + "] 完成";
                    }
                }
            }
        } catch (Exception e) {
            log.debug("解析工作流 SSE 事件失败: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        // 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 权限校验：只有创建用户本人才能部署应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署应用");
        }
        // 检查是否已有 deployKey，没有则生成随机6位（大小写字母 + 数字）
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 检查原目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成目录不存在，请先生成代码");
        }
        // Vue项目执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT){
            // 不需要异步构建，部署是可接受的耗时操作
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue项目构建成功但 dist 目录不存在");
            // 将 dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        }
        // 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (IORuntimeException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "更新应用部署信息失败");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;

    }

    @Override
    public void validApp(App app, boolean add) {
        if (app == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String appName = app.getAppName();
        String initPrompt = app.getInitPrompt();
        String codeGenType = app.getCodeGenType();
        // 创建时必填参数校验
        if (add) {
            if (StrUtil.hasBlank(appName, initPrompt)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用名称或初始化 prompt 为空");
            }
        }
        // 代码生成类型校验
        if (StrUtil.isNotBlank(codeGenType) && CodeGenTypeEnum.getEnumByValue(codeGenType) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型错误");
        }
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 填充用户信息
        Long userId = app.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUserVO(userVO);
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量查询用户信息，避免 N+1 查询
        Set<Long> userIdSet = appList.stream().map(App::getUserId).collect(Collectors.toSet());
        List<User> userList = userService.listByIds(userIdSet);
        Map<Long, List<User>> userIdUserListMap = userList.stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        return appList.stream().map(app -> {
            AppVO appVO = new AppVO();
            BeanUtil.copyProperties(app, appVO);
            Long userId = app.getUserId();
            if (userIdUserListMap.containsKey(userId)) {
                appVO.setUserVO(userService.getUserVO(userIdUserListMap.get(userId).get(0)));
            }
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }

    @Override
    public void generateAppScreenshotAsync(Long appId, String appDeployUrl) {
        Thread.startVirtualThread(() -> {
            // 截图并获取图片URL
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appDeployUrl);
            // 更新封面字段
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updateResult = this.updateById(updateApp);
            ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "更新应用封面图片失败");
        });
    }
}
