FROM maven:3.9-eclipse-temurin-17 AS b
WORKDIR /a
COPY . .
RUN mvn -q package -DskipTests
FROM eclipse-temurin:17-jre
COPY --from=b /a/target/*.jar /app.jar
CMD ["java","-jar","/app.jar"]
