FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/maven-cicd-demo.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
