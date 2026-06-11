# service-user 런타임 이미지 — bootJar 기반 (JDK 21)
#
# 빌드 전제: 호스트에서 먼저 bootJar를 만든다.
#   cd qtai-server && ./gradlew :service-user:bootJar
# 그 다음 이 디렉터리를 컨텍스트로 이미지를 빌드한다(docker-compose가 자동 수행).
#
# 보안: 비루트 사용자(spring)로 실행, 평문 키/시크릿은 이미지에 넣지 않고 런타임 env로 주입.
FROM eclipse-temurin:21-jre-jammy

# 비루트 사용자 생성 (CLAUDE.md §9 보안 기준)
RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app

# 사전 빌드된 bootJar만 복사한다.
# bootJar = *-SNAPSHOT.jar, plain jar(*-SNAPSHOT-plain.jar)는 글롭에 매칭되지 않아 제외된다.
ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
COPY --chown=spring:spring ${JAR_FILE} app.jar

USER spring:spring

# 사용자/인증 서비스 포트(기본 8081)
EXPOSE 8081

# 컨테이너 메모리에 맞춰 힙 자동 조정. 헬스체크는 compose/k8s에서 TCP로 정의.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
