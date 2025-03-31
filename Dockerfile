FROM gradle:8.13-jdk21 as build
WORKDIR /app
COPY . /app/
RUN gradle build --no-daemon

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/task-manager-1.0-SNAPSHOT.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]