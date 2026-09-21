FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build
ENV MAVEN_OPTS="-Dfile.encoding=UTF-8"
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw
RUN ./mvnw -B -Dmaven.test.skip=true dependency:go-offline

COPY src src
RUN ./mvnw -B -Dmaven.test.skip=true clean package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
ENV TZ=Asia/Shanghai
ENV SERVER_PORT=8089
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

COPY --from=builder /build/target/*.jar /app/app.jar

EXPOSE 8089

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
