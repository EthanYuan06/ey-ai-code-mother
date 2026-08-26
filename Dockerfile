# ============================================
# 阶段1: 构建 - 使用 JDK 21 + Maven 编译
# ============================================
FROM eclipse-temurin:21-jdk AS build

RUN apt-get update && \
    apt-get install -y maven --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /build
COPY pom.xml .
# 先下载依赖，利用 Docker 层缓存
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ============================================
# 阶段2: 运行 - JRE 21 + Chromium (Selenium 截图)
# ============================================
FROM eclipse-temurin:21-jre

# 安装 Chromium 及其依赖（Selenium 网页截图用）
RUN apt-get update && \
    apt-get install -y chromium --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*

# 告诉 WebDriverManager 使用系统 Chromium
ENV CHROME_BIN=/usr/bin/chromium

# 创建非 root 用户
RUN groupadd -r appuser && useradd -r -g appuser -d /app -s /sbin/nologin appuser

WORKDIR /app

# 从构建阶段复制 JAR
COPY --from=build /build/target/*.jar app.jar

# 创建临时文件目录（代码生成产物、截图等）
RUN mkdir -p /app/tmp && chown -R appuser:appuser /app

USER appuser

EXPOSE 8123

ENTRYPOINT ["java", "-jar", "app.jar"]
