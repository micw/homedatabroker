FROM openjdk:17-alpine

ADD target/homedatabroker-1.0.0-SNAPSHOT.jar /app/homedatabroker.jar

ENV TZ=Europe/Berlin

WORKDIR /app

ENTRYPOINT ["java","-XX:+UnlockExperimentalVMOptions","-XX:+UseContainerSupport","-jar","/app/homedatabroker.jar"]
