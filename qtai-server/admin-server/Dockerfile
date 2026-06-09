# admin-server 런타임 이미지 — bootJar 기반 (JDK 21)
#
# 빌드 전제: 호스트에서 먼저 bootJar를 만든다.
#   cd qtai-server && ./gradlew :admin-server:bootJar
#
# admin-server는 모놀리식 복사본(admin 컨트롤러만 잔류)이라 Flyway로 단일 DB 스키마를 구성하고
# JWT 개인키/공개키를 모두 요구한다(prod 프로파일). 시크릿은 런타임 env로만 주입(CLAUDE.md §7,§9).
FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app

ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
COPY --chown=spring:spring ${JAR_FILE} app.jar

USER spring:spring

# 관리자 서버 포트(기본 8090)
EXPOSE 8090

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
