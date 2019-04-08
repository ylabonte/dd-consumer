![OpenJDK11](https://img.shields.io/badge/OpenJDK-11-orange.svg?style=flat&logo=java&logoColor=white)
![Docker Cloud Automated build](https://img.shields.io/docker/cloud/automated/labonte/ddc.svg?logo=docker&logoColor=white)
![Docker Cloud Build Status](https://img.shields.io/docker/cloud/build/labonte/ddc.svg?logo=docker&logoColor=white)

# dd-consumer - <small>a simple Download Delegation Consumer</small>

A [Spring](https://spring.io/) based web service consuming delegated
downloads ([see json schema here](https://github.com/ylabonte/dd-json-schema))
which are http `POST`ed as `Content-Type: application/json; charset=utf-8`
to the root route `http://<service-address>:1040/`.
A simple `GET` request to the same route gives a brief status of all
known downloads (current and history).

For a corresponding Chrome browser extension which is capable of 
delegating downloads from your browser to your instance of the ddc see: 
https://github.com/ylabonte/dd-chrome-extension

This implementation is rudimentary and offers absolutely no security 
features! I strongly discourage from productive use (accessible from
the internet)!


## Run using docker via dockerhub
You can run the app by simply using the
[prebuilt docker image](https://hub.docker.com/r/labonte/ddc) from
dockerhub:
```bash
$ docker run -it --rm --name ddc \
     -v ${HOME}/Downloads:/root/Downloads \
     -p 1040:1040 labonte/ddc:latest \
```


## Run using self built docker image

Simply clone the repo, build the image and run the container as usual.
```bash
$ git clone https://github.com/ylabonte/ddc.git ddc
$ cd ddc
$ docker build -f docker/Dockerfile ddc:latest .
$ docker run -it --rm --name ddc \
     -v ${HOME}/Downloads:/root/Downloads \
     -v $(pwd)/application.properties:/root/application.properties \
     -p 1040:1040 ddc:latest
``` 


## Configuration
Actually no configuration is needed. Nevertheless, it is possible to
overwrite the default configuration using Docker's volume parameter `-v`
(see the example above). You can set any Spring Boot configuration
parameters you want.

Custom application settings are:

| Setting | Default Value | Description |
|---|---|---|
| `download.basePath` | ${HOME}/Downloads | Target directory for downloaded files. Please note: This parameter only affects the path inside the container. You should use the corresponding volume (see the example above) to mount an arbitrary directory of your host/storage system. |


## API

### Getting status
```bash
$ curl 'localhost:1040'
```

### Requesting download(s)
```bash
$ curl 'localhost:1040' \
-H 'Content-Type: application/json; charset=utf-8' \
-d '{
  "url": "http://example.com/info.txt",
  "header": ["Cookie: auth-session-cookie=some-hash"],
  "destination": "very-important/info.txt"
}'
```
This will try to download the file from `http://example.com/info.txt`
using the supplied http cookie header and saves it to
`very-important/info.txt` (relative to the configured output path).


## Development

### Requirements
* Java 11

### Build
```bash
$ ./gradlew build
```
This will (re)build the project.

### Test
```bash
$ ./gradlew test
```
Run all tests.

### Run
```bash
$ ./gradlew run
```
This will build, test and run the project for you.
