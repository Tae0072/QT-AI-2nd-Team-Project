# service-note 런타임 이미지 — bootJar 기반 (JDK 21)
#
# 빌드 전제: 호스트에서 먼저 bootJar를 만든다.
#   cd qtai-server && ./gradlew :service-note:bootJar
#
# 보안: 비루트 사용자(spring)로 실행, 평문 키/시크릿은 런타임 env로만 주입.
FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app

ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
COPY --chown=spring:spring ${JAR_FILE} app.jar

USER spring:spring

# 노트/나눔/신고 서비스 포트(기본 8083)
EXPOSE 8083

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
