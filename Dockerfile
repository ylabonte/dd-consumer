FROM gradle:5.2-jdk8 as builder
ARG build_version
ENV BUILD_VERSION $build_version

COPY . /app
WORKDIR /app
USER root
RUN gradle
RUN gradle bootJar
RUN echo "export BUILD_VERSION=$BUILD_VERSION" > build/libs/build.version


FROM java:8-alpine
ENV BUILD_VERSION $build_version
COPY --from=builder /app/build/libs/* /root/
COPY ./docker-entrypoint.sh /
RUN chmod u+x /docker-entrypoint.sh
RUN apk --no-cache add ca-certificates
RUN rm -rf /var/cache/apk/*
VOLUME /root/Downloads
VOLUME /root/application.settings
WORKDIR /root

ENTRYPOINT /docker-entrypoint.sh

EXPOSE 1040
