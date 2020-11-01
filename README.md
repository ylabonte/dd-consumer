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
A simple `GET` request to the server root will return an object with an
overview of all registered downloads. Those downloads may be listed
multiple times under different keys. You can use the following example
to pretty print the current status object to your console or just open
the address in your favorite browser.
```bash
$ curl 'localhost:1040' | jq
```

The response should look something similar to:
```json
{
  "downloads": [
    {
      "id": 38,
      "url": "https://material.io/tools/icons/static/icons/baseline-cloud_download-24px.svg",
      "method": "GET",
      "destination": "baseline-cloud_download-24px_1.svg",
      "size": -1,
      "hSize": "0 B",
      "status": "SUCCEEDED",
      "progress": 0,
      "speed": 0,
      "hSpeed": "0 B/s",
      "fileSize": 0,
      "hFileSize": "0 B",
      "erroneous": false,
      "message": ""
    }
  ]
}
```

#### Status Object
| Key | Value | Description |
|---|---|---|
| `downloads` | `Download[]` | A list of all registered downloads.

#### Download Object
| Key | Value | Description |
|---|---|---|
| `id` | `number` | Unique identifier (per instance). |
| `url` | `string` | Download URL. Target URL in case of redirects. |
| `method` | `string` | HTTP request method. |
| `destination` | `string` | Download destination (relative to the configured download base path). |
| `size` | `number` | File size given by the HTTP Content-Length header. |
| `hSize` | `string` | Same as `size` but as human readable string with appropriate unit. |
| `status` | `string` | One of `UNINITIALIZED`, `INITIALIZED`, `WAITING`, `PROGRESSING`, `PAUSED`, `SUCCEEDED`, `FAILED`, `ABORTED`. |
| `progress` | `number` | Floating point number between 0 and 1 indicating the progress. |
| `speed` | `number` | Number of bytes written in the last 200ms. |
| `hSpeed` | `string` | Same as `speed` but as human readable string with appropriate unit. |
| `fileSize` | `number` | Actual file size in bytes. |
| `hFileSize` | `string` | Same as `fileSize` but as human readable string with appropriate unit. |
| `erroneous` | `boolean` | Indicates whether an error occurred. |
| `message` | `string` | Error message when `erroneous` is `true`. |


### Adding a download


```bash
$ curl 'localhost:1040' \
-H 'Content-Type: application/json; charset=utf-8' \
-d '[{
  "url": "http://example.com/info.txt",
  "header": ["Cookie: auth-session-cookie=some-hash"],
  "destination": "very-important/info.txt"
}]' | json_pp
```
This will try to download the file from `http://example.com/info.txt`
using the supplied http cookie header and saves it to
`very-important/info.txt` (relative to the configured output path).


## Development
It's open source, so you may simply clone or fork the repository to 
perform some customizations or what ever you want to do.
You are very welcome to stage a pull request.  

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
