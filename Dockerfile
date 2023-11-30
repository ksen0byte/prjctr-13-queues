# Stage 1: Build the JAR using Scala CLI
FROM virtuslab/scala-cli:latest as build

# Set the working directory in the container
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY Main.scala /app/Main.scala

# Use Scala CLI to package your application into an assembly (JAR)
RUN scala-cli --power package . --assembly -o app.jar

# Stage 2: Create a new stage with a Java runtime to run the JAR
FROM openjdk:21

# Set the working directory in the container
WORKDIR /app

# Copy the assembly JAR from the previous stage into this new stage
COPY --from=build /app/app.jar /app/

# Command to run the application
CMD ["java", "-jar", "/app/app.jar"]
