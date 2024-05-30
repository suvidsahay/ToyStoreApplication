# Use a base image
FROM openjdk:latest

# Set the working directory in the container
WORKDIR /src/frontendservice

# Copy the compiled Java classes and any necessary resources
COPY . .

# Compile Java code
# RUN javac -d . *.java
RUN javac handler/*.java *.java

# Debug issue 
WORKDIR /src

# Specify the command to run the Java application
CMD ["java", "frontendservice.FrontEndService"]
