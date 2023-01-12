FROM gcr.io/distroless/java17 as build
COPY core/target/*.jar app.jar
EXPOSE 9091
EXPOSE 9092
USER 65534:65534
ENTRYPOINT ["java", "--add-opens=java.base/java.net=ALL-UNNAMED", "-jar","-Djava.security.egd=file:/dev/./urandom", "-Dlogging.file.path=/log", "-Dspring.profiles.active=prod", "app.jar"]