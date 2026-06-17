FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring \
	&& useradd --system --gid spring --home-dir /app spring

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
