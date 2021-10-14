FROM openjdk:11-jre-slim
WORKDIR /usr/app
ENTRYPOINT ["java", "-jar", "entrypoint.jar"]
COPY target/terraform-*-jar-with-dependencies.jar /usr/app/entrypoint.jar
