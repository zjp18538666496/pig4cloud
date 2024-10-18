#基础镜像
FROM m.daocloud.io/docker.io/ubuntu

#工作目录
WORKDIR /servers

#jdk
ADD jdk-17.0.13_linux-x64_bin.tar.gz .

#配置环境变量
ENV JAVA_VERSION=17.0.13
ENV JAVA_HOME=/servers/jdk-17.0.13
ENV PATH=$JAVA_HOME/bin:$PATH

ENTRYPOINT ["java", "-jar", "/app/pig4cloud-1.0-SNAPSHOT.jar"]