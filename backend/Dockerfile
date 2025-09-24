# 使用 OpenJDK 17 作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制 Maven 包装器和 pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# 给 mvnw 执行权限
RUN chmod +x ./mvnw

# 下载依赖
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用
RUN ./mvnw clean package -DskipTests

# 暴露端口
EXPOSE 8080

# 运行应用
CMD ["java", "-jar", "target/warehouse-management-system-1.0.0.jar"]
