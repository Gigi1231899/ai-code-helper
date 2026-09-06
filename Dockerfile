# =============================================================
# 生产部署用 Dockerfile（与开发用的 DockerFile 分开）
#
# 更推荐直接用 docker-compose（已内置 MySQL + 自动建表）：
#   docker compose up -d
# =============================================================

# ---------- Stage 1: 用 Maven 镜像编译打包 ----------
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# 先只复制 pom.xml，利用 Docker 层缓存，依赖不变时不重复下载
COPY pom.xml .
RUN mvn -B dependency:go-offline

# 再复制源码打包
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: 运行时镜像 ----------
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/eclipse-temurin:21-jre-jammy

WORKDIR /app

# 只把 jar 复制到运行时镜像，不携带 Maven 与源码，镜像更小也更安全
COPY --from=build /app/target/ai-code-helper-*.jar ./app.jar

# 数据目录：用户知识库文件 + 每用户向量库（必须挂载到宿主机持久化）
RUN mkdir -p /app/data && chmod 755 /app/data
VOLUME /app/data

# 使用 prod 配置，密钥全部走环境变量（见 src/main/resources/application-prod.yml）
ENV SPRING_PROFILES_ACTIVE=prod
# 容器环境 JVM 参数：让 JVM 感知容器内存限制
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8090

# 用 sh -c 包一层，才能让 $JAVA_OPTS 环境变量生效
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar ./app.jar"]
