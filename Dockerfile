FROM maven:3.9.6-eclipse-temurin-17

WORKDIR /app

# Pre-fetch dependencies for faster runs
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Copy rest of the project
COPY . .

# Default to an interactive shell
CMD ["bash"]