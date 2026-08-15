
# Build stage

FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build


COPY pom.xml .

RUN mvn dependency:go-offline -B


COPY src ./src


RUN mvn clean package -DskipTests



# Runtime stage

FROM eclipse-temurin:21-jre

WORKDIR /app

# Güvenlik açısından root olmayan kullanıcı
RUN useradd -r -u 1001 springuser

COPY --from=builder /build/target/*.jar app.jar

RUN chown springuser:springuser app.jar

USER springuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]