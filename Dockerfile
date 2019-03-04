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
ENV BUILD_VERSION $build_version
COPY --from=builder /app/build/libs/* /root/
COPY ./docker-entrypoint.sh /
RUN chmod u+x /docker-entrypoint.sh
VOLUME /root/Downloads
VOLUME /root/application.settings
WORKDIR /root

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["start"]

EXPOSE 1040
