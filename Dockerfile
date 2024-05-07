#
# Build stage
#
FROM eclipse-temurin:17-jdk-jammy AS build
ENV HOME=/usr/app
RUN mkdir -p $HOME
WORKDIR $HOME
ADD . $HOME
RUN apt-get update && apt-get install maven -y
RUN mvn -f $HOME/pom.xml clean package

#
# Package stage
#
FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=/usr/app/target/*.jar
COPY --from=build $JAR_FILE /app/runner.jar
RUN apt-get update && apt-get -y install git
RUN git config --global http.sslVerify false
EXPOSE 8080
ENTRYPOINT java -jar /app/runner.jar