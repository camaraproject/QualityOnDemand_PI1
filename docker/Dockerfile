FROM public.ecr.aws/amazoncorretto/amazoncorretto:21.0.5 AS build
COPY core/target/*.jar app.jar
EXPOSE 9091
EXPOSE 9092
USER 65534:65534

ENTRYPOINT java \
  --add-opens=java.base/java.net=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  --add-opens=java.base/java.time=ALL-UNNAMED \
  --add-opens=java.base/java.time.zone=ALL-UNNAMED \
  -Djava.security.egd=file:/dev/./urandom \
  -Dlogging.file.path=/log \
  -jar app.jar
