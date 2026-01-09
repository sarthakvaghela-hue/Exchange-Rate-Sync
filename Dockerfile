FROM maven:3.9.6-eclipse-temurin-17

WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:resolve

COPY . .
RUN chmod +x run_tests.sh

ENTRYPOINT ["./run_tests.sh"]