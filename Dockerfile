# ============================================
# 阶段1: 构建 - Maven + JDK21（官方镜像已预装，无需 apt）
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
# 先下载依赖，利用 Docker 层缓存
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ============================================
# 阶段2: 运行 - JRE 21 (Ubuntu noble) + Google Chrome
# ============================================
FROM eclipse-temurin:21-jre

# 安装 Google Chrome 稳定版（Selenium 网页截图用）
# fonts-noto-cjk：中文字体，避免 headless Chrome 截图时中文显示为方框（豆腐块）
# fonts-noto-color-emoji：彩色 emoji 字体，避免截图时 emoji 显示为方框
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

# 创建非 root 用户
RUN groupadd -r appuser && useradd -r -g appuser -d /app -s /sbin/nologin appuser

WORKDIR /app

# 从构建阶段复制 JAR（用通配符，避免写死 artifact 名）
COPY --from=build /build/target/*.jar app.jar

# 创建临时文件目录（代码生成产物、截图等）
RUN mkdir -p /app/tmp && chown -R appuser:appuser /app

USER appuser

EXPOSE 8123

ENTRYPOINT ["java", "-jar", "app.jar"]
