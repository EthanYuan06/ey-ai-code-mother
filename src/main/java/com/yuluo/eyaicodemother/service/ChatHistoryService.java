package com.yuluo.eyaicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yuluo.eyaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.yuluo.eyaicodemother.model.entity.ChatHistory;
import com.yuluo.eyaicodemother.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author EthanYuan
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 从数据库加载历史对话到内存
     *
     * @param appId 应用id
     * @param chatMemory 对话记忆
     * @param maxCount 最大加载数量
     * @return 加载数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 根据应用id分页获取对话消息
     *
     * @param appId 应用id
     * @param pageSize 每页大小
     * @param lastCreateTime 上次创建时间
     * @param loginUser 当前登录用户
     * @return 对话消息
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);
    /**
     * 添加对话消息
     *
     * @param appId 应用id
     * @param message 消息
     * @param messageType 消息类型
     * @param userId 当前登录用户id
     * @return 添加结果
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据应用id删除对话消息
     *
     * @param appId 应用id
     * @return 删除结果
     */
    boolean deleteByAppId(Long appId);

    /**
     * 获取查询条件
     *
     * @param chatHistoryQueryRequest 查询条件
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
