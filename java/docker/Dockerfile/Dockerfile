# 先在宿主机执行 mvn -DskipTests package 生成 target/pig4cloud-1.0-SNAPSHOT.jar
# 构建阶段：按Spring Boot分层解压jar，依赖层与应用层分离，充分利用镜像缓存
FROM m.daocloud.io/docker.io/eclipse-temurin:17-jre AS extraction
WORKDIR /app
COPY target/pig4cloud-1.0-SNAPSHOT.jar ./app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# 运行阶段
FROM m.daocloud.io/docker.io/eclipse-temurin:17-jre
WORKDIR /app
COPY --from=extraction /app/extracted/dependencies/ ./
COPY --from=extraction /app/extracted/spring-boot-loader/ ./
COPY --from=extraction /app/extracted/snapshot-dependencies/ ./
COPY --from=extraction /app/extracted/application/ ./
ENV TZ=Asia/Shanghai \
    LOG_HOME=/app/logs
# 日志目录（logback-spring.xml写入/app/logs），生产挂载卷持久化：-v pigx-logs:/app/logs
VOLUME /app/logs
EXPOSE 9000
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
