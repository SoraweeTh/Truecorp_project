
# Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
# Click nbfs://nbhost/SystemFileSystem/Templates/Other/Dockerfile to edit this template

#############################################################

# Use official base image of Java Runtim
FROM openjdk:11-jdk
 
# Set volume point to /tmp
VOLUME /tmp
 
# Make port 8080 available to the world outside container
EXPOSE 8080
 
# Set application's JAR file
ARG JAR_FILE=target/*.jar
 
# Add the application's JAR file to the container
ADD ${JAR_FILE} app.jar
 
# Run the JAR file
ENTRYPOINT java $JAVA_OPTS -jar /app.jar
