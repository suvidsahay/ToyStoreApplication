# Use a base image
FROM openjdk:latest

# Set the working directory in the container
WORKDIR /src/catalogservice

# Copy the compiled Java classes and any necessary resources
COPY . .

# Compile Java code
RUN javac -d . *.java

# Specify the command to run the Java application
CMD ["java", "catalogservice.CatalogService"]
