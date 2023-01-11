FROM gcr.io/distroless/java17 as build
COPY core/target/*.jar app.jar
EXPOSE 9091
USER 65534:65534
ENTRYPOINT ["java","-Dhttp.proxyHost=10.105.124.65","-Dhttp.proxyPort=8081","-Dhttps.proxyHost=10.105.124.65","-Dhttps.proxyPort=8081","-Dhttps.proxySet=true","-Dhttp.proxySet=true","-jar","-Djava.security.egd=file:/dev/./urandom", "-Dlogging.file.path=/log", "-Dspring.profiles.active=prod", "app.jar"]