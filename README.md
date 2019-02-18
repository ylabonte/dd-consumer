# Simple Download Delegation Consumer

A [Spring](https://spring.io/) based web service consuming delegated
downloads ([see json schema here](https://gist.github.com/ylabonte/79d36b4f17635d7661bcac75677cd216#file-downloadrequests-schema-json))
which are http `POST`ed as `Content-Type: application/json; charset=utf-8`
to the root route `http://<service-address>:1040/`.
A simple `GET` request to the same route gives a brief status of all
known downloads (current and history).

For an appropriate Chrome browser extension see: https://github.com/ylabonte/chrome-simple-download-delegator

**At current development state, there is no real download processing
implemented yet!**

## Requirements
* Java 8
* Gradle 5

## Run
```bash
$ gradle bootRun
```
This will build, test and run the project for you.

## Build
This will (re)build the project (`bootRun` also does). Useful if you
only want the JAR file for execution.
```bash
$ gradle build
```

## Test
Only run tests. Useful during development.
```bash
$ gradle test
```

## Request status
```bash
$ curl 'localhost:1040'
```

## Request download(s)
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