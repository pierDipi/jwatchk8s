FROM openjdk:11 as builder

WORKDIR /app

# download dependencies
COPY build.gradle settings.gradle gradlew ./
COPY gradle/ ./gradle
RUN ./gradlew build || return 0

# Build the project
COPY . .
RUN ./gradlew build

FROM openjdk:11
COPY --from=builder /app/build/libs/jconfigwatchk8s-1.0-SNAPSHOT.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]