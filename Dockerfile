FROM gradle:5.2-jdk11 as builder
ARG build_version
ENV BUILD_VERSION $build_version

COPY . /app
WORKDIR /app
USER root
RUN gradle
RUN gradle bootJar
RUN echo "${BUILD_VERSION}" > build/libs/build.version


FROM openjdk:11-jre-slim
ENV JAVA_ARGS -Xms64m -Xmx768m
RUN apt-get update && apt-get -y install libtcnative-1=1.2.21-1~bpo9+1
COPY --from=builder /app/build/libs /app
COPY ./docker-entrypoint.sh /
VOLUME /root/Downloads
WORKDIR /app

ENTRYPOINT ["/bin/sh", "/docker-entrypoint.sh"]
CMD ["start"]

EXPOSE 1040
