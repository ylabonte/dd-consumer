FROM gradle:5.3-jdk11 as builder
ARG build_version=latest
ENV BUILD_VERSION $build_version

COPY . /app
WORKDIR /app
USER root
RUN gradle && gradle bootJar
RUN echo $build_version > build/libs/build.version


FROM openjdk:11-jre-slim
ENV JAVA_ARGS -Xms64m -Xmx768m
RUN apt-get update &&\
    apt-get -y install libtcnative-1=1.2.21-1~bpo9+1 &&\
    apt-get -y clean
COPY --from=builder /app/build/libs /app
COPY docker/docker-entrypoint.sh /
VOLUME /root/Downloads
WORKDIR /app

RUN printf "export BUILD_VERSION=%s\n" $(cat build.version) >> /etc/environment
ENTRYPOINT ["/bin/sh", "/docker-entrypoint.sh"]
CMD ["start"]

EXPOSE 1040
