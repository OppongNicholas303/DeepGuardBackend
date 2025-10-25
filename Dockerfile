FROM openjdk:17-jdk-slim

RUN apt-get update && apt-get install -y \
    libopencv-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests

RUN mkdir -p /app/uploads

EXPOSE 9080

CMD ["java", "-jar", "target/document-analysis-1.0.0.jar"]