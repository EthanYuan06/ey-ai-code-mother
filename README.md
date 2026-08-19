# Code Zero Studio

> 一句话轻松创建网站应用 —— AI 驱动的零代码应用生成平台

## 一、项目介绍

（居中展示项目LOGO）

**Code Zero Studio** 是一个基于 Spring Boot 3 + LangChain4J + LangGraph4J 的 AI 零代码应用生成平台。用户只需用自然语言描述（如"帮我创建一个个人博客网站"），AI即可自动理解需求、智能选择生成模式，并实时输出前端代码，支持在线预览、一键部署和代码下载。

本项目典型应用场景如下：

产品经理：用 AI 快速生成原型进行需求验证

开发人员：快速提供页面原型，可按需修改样式与交互逻辑，降低开发成本

个人用户：可打造个性化精美网页，例如个人博客、网页简历、作品集网站等

## 二、核心功能

Code Zero Studio 拥有四大核心能力，具体如下：

#### 1、智能代码生成

用户使用自然语言描述需求，AI即可根据项目复杂度智能选择代码生成模式（单文件/多文件/Vue工程），通过工具调用生成、保存文件，并基于流式输出让用户实时观看代码与网页生成结果。

（代码生成截图）

#### 2、可视化修改

生成的应用实时展示，用户可进入编辑模式，选中元素，并进行多轮对话，不断迭代网页样式，直到满足用户需求。

（可视化修改截图）

#### 3、一键部署

可将生成好的项目一键部署到云端，部署时自动截取项目封面图，部署完成后返回可访问的URL，同时支持将项目源码打包下载

（返回URL的窗口、平台主页截图）

#### 4、后台监控体系

提供用户管理、应用管理、系统性能指标、业务指标等后台管理功能，管理员可通过监控面板实时查看系统性能、业务数据及设置精选应用。

(后台监控截图)

---

## 三、技术选型

### 后端

核心框架：

- Spring Boot 3：项目开发框架
- Java 21：虚拟线程支持，提升并发能力

微服务：

- Spring Cloud Alibaba：微服务框架
- Dubbo RPC：服务远程调用、负载均衡
- Nacos：注册中心
- Higress：网关

AI 技术：

- LangChain4J：提供 AI 能力，快速开发AI应用
- LangGraph4J：编排 AI 工作流
- Tool Calling：代码增删改操作支持
- DeepSeek：主模型，负责需求理解、代码生成
- Qwen：负责文生图、多模态能力支持

数据访问与存储：

- MyBatis-Flex：轻量ORM框架，开发数据访问层
- MySQL：存储用户、应用、对话记忆数据
- Redis：缓存登录会话、AI对话记忆
- Caffeine：本地缓存AI Service服务实例
- 腾讯云COS：图片数据云端托管

工具库：

- Hutool：业务工具库
- Lombok：数据模型开发
- Knife4j：接口文档
- Redisson：分布式限流
- Jsoup：网页解析
- Selenium：网页截图



### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| **Vue** | 3.5.x | 前端框架 |
| **TypeScript** | 5.8.x | 类型安全 |
| **Vite** | 7.x | 构建工具 |
| **Ant Design Vue** | 4.x | UI 组件库 |
| **Pinia** | 3.x | 状态管理 |
| **Vue Router** | 4.x | 路由管理 |
| **Axios** | 1.x | HTTP 请求 |
| **markdown-it** | 14.x | Markdown 渲染 |
| **highlight.js** | 11.x | 代码高亮 |

---

## 四、设计亮点

### 4.1 设计模式驱动的核心架构

项目核心 AI 代码生成模块采用多种设计模式协同工作，实现了高内聚、低耦合的可扩展架构：

- **策略模式（Strategy Pattern）**：`CodeParser` 接口定义统一的代码解析入口，`HtmlCodeParser` / `MultiFileCodeParser` 各自封装特定类型的解析算法，新增生成类型只需新增策略类
- **模板方法模式（Template Method Pattern）**：`CodeFileSaverTemplate` 固化保存流程骨架（校验 → 建目录 → 写文件 → 返回），子类实现具体文件写入逻辑，消除重复代码
- **执行器模式（Executor Pattern）**：`CodeParserExecutor` / `CodeFileSaverExecutor` / `StreamHandlerExecutor` 作为统一路由层，根据类型枚举选择对应策略/模板实例，避免外层 if-else 膨胀
- **门面模式（Facade Pattern）**：`AiCodeGeneratorFacade` 回归纯编排层，组合调用执行器完成解析和落盘，调用方无需关心内部细节

```
用户需求 → AiCodeGeneratorFacade（编排）
         → CodeParserExecutor（路由）→ 具体 Parser 策略（解析）
         → CodeFileSaverExecutor（路由）→ 具体 Saver 模板（落盘）
```

### 4.2 全链路 SSE 流式响应

从 AI 模型到前端的全链路流式传输：

- 后端通过 LangChain4j 的 `Flux<String>` / `TokenStream` 接收 AI 流式输出
- 使用 Spring WebFlux 的 `ServerSentEvent` 将数据以 SSE 格式推送至前端
- 前端通过 `EventSource` 实时接收并渲染，实现打字机效果
- 全局异常处理器针对 SSE 请求特殊适配，确保错误信息也能以 SSE 格式返回

### 4.3 AI Agent 工具调用（Vue 工程模式）

Vue 工程模式下，AI 具备 Agent 能力，可通过工具调用直接操作文件系统：

- **FileWriteTool**：写入文件
- **FileReadTool**：读取文件内容
- **FileModifyTool**：修改已有文件内容（局部替换）
- **FileDeleteTool**：删除文件
- **ToolManager**：统一管理所有工具注册与发现
- 支持幻觉工具名拦截策略，防止 AI 调用不存在的工具

### 4.4 虚拟线程异步化处理

利用 Java 21 虚拟线程特性实现高性能异步操作：

- 应用部署后异步生成截图并上传至对象存储，不阻塞主流程
- Vue 项目构建（npm install + npm run build）在虚拟线程中异步执行
- 轻量级线程模型，适合大量 I/O 密集型任务

### 4.5 基于注解 + AOP 的权限控制

- 自定义 `@AuthCheck` 注解声明接口所需角色
- AOP 切面 `AuthInterceptor` 统一拦截，实现 RBAC 权限模型
- 支持 `user` / `admin` 两级角色，管理员接口自动鉴权
- 业务层通过 `getLoginUser` 获取当前用户，实现数据级权限校验（如只有应用创建者可编辑/删除/下载）

### 4.6 AI Service 实例缓存与多会话隔离

- 使用 Caffeine 本地缓存管理 AI Service 实例，按 `appId` 隔离会话
- 每个应用独立维护 `MessageWindowChatMemory`（基于 Redis 持久化），支持多轮对话上下文
- 应用创建时自动从数据库加载历史对话到内存，实现会话恢复
- 不同代码生成类型自动选择不同模型配置（推理模型 / 普通模型）

### 4.7 批量查询优化（N+1 问题解决）

- `getAppVOList` 方法采用批量查询用户信息替代逐条查询
- 使用 `Set` 收集去重后一次性 `listByIds`，避免经典 N+1 性能问题

### 4.8 SSE 错误优雅处理

- 全局异常处理器识别 SSE 请求（通过 `Accept` 头或 URL 路径）
- 错误信息以 SSE 事件格式（`event: business-error`）返回，而非标准 JSON
- 前端针对 `business-error` 事件专门处理，确保流式连接中的错误也能被正确展示

---

## 项目结构概览

```
ey-ai-code-mother/
├── src/main/java/com/yuluo/eyaicodemother/
│   ├── ai/                    # AI 模块（模型接口、工具、工厂）
│   ├── annotation/            # 自定义注解（@AuthCheck）
│   ├── aop/                   # AOP 切面（权限拦截）
│   ├── common/                # 通用类（BaseResponse、ResultUtils）
│   ├── config/                # 配置类（模型、CORS、Session）
│   ├── constant/              # 常量定义
│   ├── controller/            # 控制层（App、User、ChatHistory、StaticResource）
│   ├── core/                  # 核心模块（门面、解析器、保存器、流处理器、构建器）
│   │   ├── AiCodeGeneratorFacade.java
│   │   ├── parser/            # 策略模式 - 代码解析
│   │   ├── saver/             # 模板方法 - 文件保存
│   │   ├── handler/           # 流处理器
│   │   └── bulider/           # Vue 项目构建器
│   ├── exception/             # 异常处理（全局异常处理器）
│   ├── manager/               # 外部服务管理（COS）
│   ├── mapper/                # 数据访问层
│   ├── model/                 # 数据模型（Entity、DTO、VO、Enum）
│   ├── service/               # 业务逻辑层
│   └── utils/                 # 工具类（截图工具）
├── ey-ai-code-mother-frontend/
│   └── src/
│       ├── api/               # API 接口定义
│       ├── components/        # 公共组件
│       ├── pages/             # 页面（首页、对话页、编辑页、管理页）
│       ├── router/            # 路由配置
│       ├── stores/            # 状态管理（Pinia）
│       └── utils/             # 工具函数
└── sql/
    └── init.sql               # 数据库初始化脚本
```
