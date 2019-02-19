FROM gradle:5.2-jdk8 as builder

COPY . /app
WORKDIR /app
USER root
RUN gradle bootJar


FROM java:8-alpine
RUN apk --no-cache add ca-certificates
VOLUME /root/Downloads
VOLUME /root/application.settings
WORKDIR /root
COPY --from=builder /app/build/libs/* /root/

ENTRYPOINT java -jar /root/sddc-0.1.0.jar

EXPOSE 1040
