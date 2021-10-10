FROM openjdk:11-jre-slim
WORKDIR /usr/app
ENTRYPOINT ["java", "-jar", "payara-operator.jar"]
COPY target/terraform-operator-0.1.0-jar-with-dependencies.jar /usr/app/payara-operator.jar
