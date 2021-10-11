FROM openjdk:11-jre-slim
WORKDIR /usr/app
ENTRYPOINT ["java", "-jar", "operator.jar"]
COPY target/terraform-operator-*-jar-with-dependencies.jar /usr/app/operator.jar
