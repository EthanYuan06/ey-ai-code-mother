<div align="center">   <img src="https://cdn.nlark.com/yuque/0/2026/jpeg/56443233/1788829964508-bd8a020d-7775-4921-bc2e-989cd378c428.jpeg" width="200"/>   <h1>Code Zero Studio</h1> </div>



## 🎤 项目介绍

Code Zero Studio ，基于 AI 的对话式零代码 Web 应用生成平台。用户可通过自然语言描述制作需求，由 Agent 智能选择生成模式，并基于代码生成工作流创建出可实时预览的页面，最终一键部署为可访问的 Web 应用，为开发者、设计师、产品经理等用户提供方便快捷的网页原型生成服务。

<font style="color:rgb(51, 51, 51);">本项目有 3 大核心能力：</font>

1. <font style="color:rgb(51, 51, 51);">【</font>**<font style="color:rgb(51, 51, 51);">对话交互式网页生成</font>**<font style="color:rgb(51, 51, 51);">】：用户描述需求，由 AI 智能分析网页生成策略，以工作流执行代码生成任务，通过工具调用生成代码文件、质量检查保证代码可运行，采用流式输出实时显示 AI 执行过程。</font>
2. <font style="color:rgb(51, 51, 51);">【</font>**<font style="color:rgb(51, 51, 51);">可视化修改</font>**<font style="color:rgb(51, 51, 51);">】：网页生成完毕后，会实时在对话界面展示，用户可启用编辑模式，选中网页元素，通过多轮对话进行迭代修改。</font>
3. <font style="color:rgb(51, 51, 51);">【</font>**<font style="color:rgb(51, 51, 51);">一键部署</font>**<font style="color:rgb(51, 51, 51);">】：当网页效果符合用户的需求后，可点击“一键部署”，获取到对应的公网访问链接</font>

<font style="color:rgb(51, 51, 51);">其他功能：</font>

+ <font style="color:rgb(51, 51, 51);">源码下载：用户可将源码 zip 压缩包下载到本地</font>
+ <font style="color:rgb(51, 51, 51);">主页精选：制作精良的网站，会被管理员设置为精选应用显示在首页，提升曝光度</font>

## 🔧 技术选型
【**后端开发技术**】

+ Java 21（虚拟线程、Switch 模式匹配、Record 记录类）
+ Spring Boot 3 
+ Spring Cloud Alibaba（微服务框架）
    - Higress（网关，转发请求）
    - Nacos（内部服务注册与发现）
    - Dubbo（内部服务 RPC 通信）
+ MyBatis-Flex（更轻量的 ORM 框架）

【**AI 开发技术**】

+ LangChain4j（集成 AI 能力）
+ LangGraph4j（工作流编排）
+ Tool Calling（文件读写）
+ 大模型选型：
    - deepseek-v4-flash（默认工作模型）
    - deepseek-v4-pro (thinking，Vue 模式工作模型)
    - qwen3.7-flash（路由模型）
    - 万相 2.2 文生图 SDK（Logo 设计）

【**数据存储**】

+ MySQL（应用记录 / 对话记忆持久化）
+ Redis（作品分页 / 对话记忆 / 登录态缓存）
+ Caffeine（AI 实例缓存）
+ 腾讯云 COS（图片资源存储）

【**工具库**】

+ Redisson（分布式限流）
+ Hutool（业务开发工具库）
+ Lombok（简化数据模型）
+ Knife4j（接口文档）
+ Selenium + WebDriver（自动网页截图）
+ Spring AOP（权限校验）

【**部署**】

+ Docker（容器化，服务编排）
+ 宝塔 Linux（生产环境）

【技术架构图】
![](https://cdn.nlark.com/yuque/0/2026/png/56443233/1789567839782-b20b685d-0022-4e4f-b38a-a09db706a669.png)

## 💻 项目演示
【登录页】用户初次访问项目，需要注册登录
![](https://cdn.nlark.com/yuque/0/2026/png/56443233/1788788107680-b41a867a-fb2c-42cc-999a-0236712d68cc.png)



【主页】已登录用户访问的首页
![](https://cdn.nlark.com/yuque/0/2026/png/56443233/1788789174402-85a1db64-9db3-4752-b924-e3be46de5cf1.png)



【个人作品与精选】展示用户个人作品，以及其他用户的精选作品
![](https://cdn.nlark.com/yuque/0/2026/png/56443233/1789024897630-9f252d3b-b3f6-4cfa-8a3d-90492c534d3e.png)



【制作界面】在此处，用户可使用本项目核心功能：对话交互式 AI 开发应用。界面会实时显示工作流的执行进度，执行完成后将静态页面实时展示在右侧，接下来可进行选中编辑。
![](https://cdn.nlark.com/yuque/0/2026/png/56443233/1788958304098-442cac35-b817-4858-bc95-79ecece713e1.png)



【可视化编辑】用户能针对不符合需求的网页元素进行选中，并与 AI 进行多轮对话，直到网页效果让用户满意为止。
![](https://cdn.nlark.com/yuque/0/2026/png/56443233/1789034236756-66eb4f77-b02d-45b4-b8fa-96d8a5c79392.png)



【删除选中元素，秒级修改】
![](https://cdn.nlark.com/yuque/0/2026/png/56443233/1789034278381-1f78da5e-39a0-4545-99de-8a65181d9625.png)



【一键部署】网页制作完成后，可通过一键部署获取公网链接，让别人访问你自己的作品
![](https://cdn.nlark.com/yuque/0/2026/png/56443233/1789003924485-13eb65c8-1162-4470-8531-756ca3a3f3f3.png)

## ✨ 核心亮点
+ 【项目架构】
    - 微服务：按照业务边界，将单体项目拆分为涵盖 app / user / screenshot 服务及其他组件等7 个模块，Nacos 注册内部服务，Dubbo 负责服务内部通信，Higress 负责对外转发请求，项目正常运行。
+ 【性能优化】
    - 利用 CompletableFuture 将原串行图片收集节点异步化，按类型给每个图片服务并行执行，每个任务均有异常兜底（返回空列表）保证服务稳定，该节点耗时降低 66%
    - 基于 Caffeine 本地缓存，在 AI Service 实例创建时缓存，每一次请求 AI 无需创建新实例
    - 针对多个 AI Service 使用同一个 ChatModel 实例造成 AI 并发调用阻塞的问题，使用多例模式优化，每请求一次 AI 都会创建新的 ChatModel 实例，多个用户无法同时制作网页的问题已得到解决。
    - Redisson 限流
+ 【编码设计】
    - 基于混合设计模式重构代码生成流程：
        * 门面模式：提供一个门面类，业务服务只需调用门面方法无需关心底层实现；
        * 执行器模式：通过 Switch 模式匹配，按代码生成模式对应执行相应的解析保存流程
        * 策略模式：各模式代码解析逻辑不同，通过生成模式分发到对应的解析器
        * 模板方法模式：代码保存流程固定但各生成模式具有差异
        * 工厂模式：创建不同的 ChatModel 实例以适配各类业务需求，按需求组装 AI 能力（流式输出、工具调用、思考模式等）
+ 【AI 工作流】
    - 搜图服务：给网页增加多样性，包含 Pexels 、LOGO 生成等
    - 质检修复：代码生成完毕后会进入质量检查，若 AI 判断无法运行则进行修复，最大循环 3 次，保证代码质量
    - 可扩展性：基于状态图构建的工作流，可灵活地根据业务需求调整工作节点

## 📂 项目结构
```plain
项目根目录/
├─ docker/ 											# 自定义 Nginx 配置
├─ microservice-ai/							# AI 模块，提供 AI 能力（与 app 模块整合）
├─ microservice-app/						# 应用服务，项目核心业务
├─ microservice-client/					# 内部服务，声明对内调用的接口
├─ microservice-common/					# 基础设施模块，含 CORS 配置、全局异常处理器等
├─ microservice-model/					# 数据模型模块，统一项目数据传输规范
├─ microservice-screenshot/			# 截图服务，项目部署后自动运行（仅对内）
├─ microservice-user/						# 用户服务，负责登录、注册
└─ tmp/													# 产物临时目录
   ├─ code_output/							# 代码生成落盘处（提供实时预览支持）
   └─ code_deploy/							# 网页部署目录（由 Nginx 静态托管）
```

## 🚀 快速开始
```bash
# 1. 克隆项目到本地
git clone https://github.com/EthanYuan06/ey-ai-code-mother.git && cd ey-ai-code-mother

# 2. 进入微服务目录
cd ey-ai-code-mother-microservice

# 3. 一键启动项目
docker compose up -d

# 4. 访问路径
# http://localhost
# 访问前先要去 localhost:8001 手动配置 Higress 网关，否则将无法访问
```



