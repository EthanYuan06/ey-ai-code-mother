# --------------------------------------------
# 阶段 1：构建（Maven + JDK 21）
# --------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# 先只复制各模块 pom，利用 Docker 层缓存预下载依赖（源码变动时不必重新拉依赖）
COPY pom.xml ./
COPY microservice-common/pom.xml     microservice-common/
COPY microservice-model/pom.xml      microservice-model/
COPY microservice-client/pom.xml     microservice-client/
COPY microservice-ai/pom.xml         microservice-ai/
COPY microservice-user/pom.xml       microservice-user/
COPY microservice-app/pom.xml        microservice-app/
COPY microservice-screenshot/pom.xml microservice-screenshot/

# 多模块 go-offline 对尚未安装的兄弟模块会报错，用 `|| true` 容忍，仅缓存外部依赖
RUN mvn -B dependency:go-offline || true

# 再复制源码
COPY microservice-common/src     microservice-common/src
COPY microservice-model/src      microservice-model/src
COPY microservice-client/src     microservice-client/src
COPY microservice-ai/src         microservice-ai/src
COPY microservice-user/src       microservice-user/src
COPY microservice-app/src        microservice-app/src
COPY microservice-screenshot/src microservice-screenshot/src

# 构建并安装全部模块，随后仅为 3 个服务 repackage 出可执行 jar
# 两步缺一不可：
#   1) clean install —— 把库模块(common/model/client/ai)装入本地仓库，供第 2 步的 -pl 精简 reactor 解析依赖；
#   2) package + repackage —— 二者必须在【同一次 Maven 会话】执行，否则 repackage 报
#      “Source file is not available”。用 -pl 限定只处理 3 个可运行服务，避免库模块因无主类而失败。
# 插件版本 3.5.3 与父 pom 的 spring-boot.version 保持一致（升级 Spring Boot 时同步修改此处）
RUN mvn -B clean install -DskipTests \
    && mvn -B package org.springframework.boot:spring-boot-maven-plugin:3.5.3:repackage \
        -pl microservice-user,microservice-app,microservice-screenshot -DskipTests

# --------------------------------------------
# 阶段 2：运行基座（JRE 21 + 非 root 用户）
# --------------------------------------------
FROM eclipse-temurin:21-jre AS jre-base

# 创建非 root 运行用户
RUN groupadd -r appuser && useradd -r -g appuser -d /app -s /sbin/nologin appuser

WORKDIR /app

# ---- user 服务：用户/鉴权，Dubbo tri 50051，HTTP 8124 ----
FROM jre-base AS user
COPY --from=build /build/microservice-user/target/*.jar app.jar
RUN mkdir -p /app/tmp && chown -R appuser:appuser /app
USER appuser
EXPOSE 8124
ENTRYPOINT ["java", "-jar", "app.jar"]

# ---- app 服务：主后端（应用/对话/代码生成），Dubbo tri 50053，HTTP 8125 ----
FROM jre-base AS app
COPY --from=build /build/microservice-app/target/*.jar app.jar
# /app/tmp 存放代码生成产物(code_output)与部署产物(code_deploy)，通过数据卷共享给 frontend
RUN mkdir -p /app/tmp && chown -R appuser:appuser /app
USER appuser
EXPOSE 8125
ENTRYPOINT ["java", "-jar", "app.jar"]

# ---- screenshot 服务：Selenium 网页截图，需 Google Chrome，Dubbo tri 50052，HTTP 8127 ----
FROM jre-base AS screenshot
# 安装 Google Chrome 稳定版（headless 截图用）
#   fonts-noto-cjk：中文字体，避免截图中文变方框；fonts-noto-color-emoji：彩色 emoji 字体
RUN apt-get update && apt-get install -y --no-install-recommends \
        curl gnupg ca-certificates fonts-liberation fonts-noto-cjk fonts-noto-color-emoji \
    && curl -fsSL https://dl.google.com/linux/linux_signing_key.pub \
        | gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" \
        > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

# Chrome 可执行文件路径
ENV CHROME_BIN=/usr/bin/google-chrome-stable

COPY --from=build /build/microservice-screenshot/target/*.jar app.jar
# /app/tmp 供 WebDriverManager 缓存 chromedriver 及临时截图使用
RUN mkdir -p /app/tmp && chown -R appuser:appuser /app
USER appuser
EXPOSE 8127
ENTRYPOINT ["java", "-jar", "app.jar"]
