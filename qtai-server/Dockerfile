FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
# Windows 체크아웃(CRLF) gradlew도 리눅스에서 실행되도록 줄바꿈 정규화 후 빌드
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
