package de.yalab.ddc.download;

import com.fasterxml.jackson.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Scope("prototype")
@ConfigurationProperties(prefix = "download")
@JsonPropertyOrder({
    "id",
    "url",
    "method",
    "destination",
    "size",
    "hSize",
    "status",
    "progress",
    "speed",
    "hSpeed",
    "fileSize",
    "hFileSize",
    "erroneous",
    "message"
})
@JsonIgnoreProperties({
    "threadGroup",
    "contextClassLoader",
    "error",
    "stackTrace",
    "allStackTraces",
    "Threads",
    "priority",
    "interrupted",
    "daemon",
    "state",
    "alive",
    "name",
    "uncaughtExceptionHandler",
    "defaultUncaughtExceptionHandler",
    "uncaughtExceptionHandler"
})
public class Download extends Thread {

    /**
     * Placeholder message, when no error occurred
     */
    private final static String MESSAGE_OK = "";

    /**
     * Plain list of http headers to send with the download request
     */
    private List<String> header = new ArrayList<>();

    /**
     * URL string
     */
    private String urlString;

    /**
     * java.net.URL class instance
     */
    private URL url = null;

    /**
     * Http method to use for the request (defaults to GET)
     */
    private HttpMethod method = HttpMethod.GET;

    /**
     * Download destination path (relative to the download.basePath)
     */
    private String destination = null;

    /**
     * Download status
     */
    private Status status = Status.UNINITIALIZED;

    /**
     * Download file size in byte.
     */
    private long size = 0;

    /**
     * In case of an error, this is the exception that should give the reason
     */
    private Exception error = null;

    /**
     * Connection handle
     */
    private HttpURLConnection connection = null;

    /**
     * Output file handle
     */
    private File file = null;

    private String basePath;

    private Boolean keepExisting;

    private Logger logger = LoggerFactory.getLogger(Download.class);

    private String hash;

    private long fileSize = 0;

    private long fileSizeUpdate = 0;

    private int redirectCounter = 0;

    private int redirectCounterThreshold = 10;

    /**
     * Download status values
     */
    public enum Status {
        UNINITIALIZED,
        INITIALIZED,
        WAITING,
        PROGRESSING,
        PAUSED,
        SUCCEEDED,
        FAILED,
        ABORTED
    }

    public Download() {}

    /**
     * Initialize from DownloadRequest object
     *
     * @param request
     */
    public Download(DownloadRequest request) {
        try {
            setName("Download" + getName());
            this.urlString = request.url;
            this.header.addAll(request.header);
            if (request.method.toLowerCase().equals("post")) this.method = HttpMethod.POST;
            this.destination = request.destination;
            this.url = new URL(request.url);
            setStatus(Status.INITIALIZED);
            this.logger.info("Download initialized:\n- Name: {}\n - Source: {}\n - Destination: {}",
                    getName(),
                    this.urlString,
                    this.destination == null ? "*unspecified*" : this.destination
            );
        } catch (MalformedURLException e) {
            this.status = Status.FAILED;
            this.error = e;
        }
    }


    /**
     * Getter and setter
     */

    private void setStatus(Status newStatus) {
        this.status = newStatus;
    }

    public URL getUrl() {
        return this.url;
    }

    @JsonProperty("url")
    public String getUrlString() {
        return this.urlString;
    }

    public HttpMethod getMethod() {
        return this.method;
    }

    public String getDestination() {
        return this.destination;
    }

    public Status getStatus() {
        return this.status;
    }

    public long getSize() { return this.size; }

    private long lastMeasureValue = 0;

    private long lastMeasure = 0;

    public double getProgress() {
        if (this.size < 1 || this.destination == null) {
            return 0;
        }

        File file = Paths.get(this.basePath, this.destination).toFile();
        if (!file.exists() || !file.isFile()) {
            this.fileSize = 0;
        }

        long now = System.currentTimeMillis();
        if (this.fileSizeUpdate - now < -1000) {
            this.fileSize = file.length();
            this.fileSizeUpdate = now;
        }

        return (double) this.fileSize / (double) this.size;
    }

    public String getMessage() {
        if (isErroneous()) {
            return this.error.getClass().getName() + ": " + this.error.getMessage();
        } else {
            return MESSAGE_OK;
        }
    }

    public boolean isErroneous() {
        return (this.error != null);
    }

    public Exception getError() {
        return this.error;
    }

    public void setBasePath(String basePath) {
        if (this.basePath == null) this.basePath = basePath;
    }

    public void setKeepExisting(Boolean keepExisting) {
        if (this.keepExisting == null) this.keepExisting = keepExisting;
    }

    @JsonProperty("hSize")
    public String getHumanReadableSize() {
        if (this.connection == null) {
            return "";
        }

        return toHumanReadableSize(this.connection.getContentLengthLong());
    }

    public long getFileSize() {
        return this.fileSize;
    }

    @JsonProperty("hFileSize")
    public String getHumanReadableFileSize() {
        return toHumanReadableSize(this.fileSize);
    }

    private long getMeasureSpeedValue() {
        if (this.size == 0 || this.fileSize == 0 || this.file == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        if (lastMeasure - now < -1000) {
            this.lastMeasure = now;
            try {
                long fileSize0 = this.file.length();
                sleep(250);

                this.lastMeasureValue = (this.file.length() - fileSize0) * 4;
            } catch (Exception e) {
                this.lastMeasureValue = 0;
            }
        }

        return this.lastMeasureValue;

    }

    public long getSpeed() {
        return getMeasureSpeedValue();
    }

    @JsonProperty("hSpeed")
    public String getHumanReadableSpeed() {
        return toHumanReadableSpeed(getSpeed());
    }


    /**
     * Helper
     */

    /**
     * Return a human readable file size.
     *
     * @param bytes
     * @return
     */
    protected String toHumanReadableSize(long bytes) {
        int unit = 1024;
        if (bytes < unit) return (bytes < 0 ? 0 : bytes) + " B";
        double value;
        char prefix;
        int exp;

        try {
            exp = (int) (Math.log(bytes) / Math.log(unit));
            prefix = ("KMGTPE").charAt(exp - 1);
        } catch (IndexOutOfBoundsException e) {
            exp = 6;
            prefix = 'E';
        }

        return String.format("%.1f %ciB", bytes / Math.pow(unit, exp), prefix);
    }

    /**
     * Return a human readable data transmission rate.
     *
     * @param bytesPerSecond
     * @return
     */
    protected String toHumanReadableSpeed(long bytesPerSecond) {
        int unit = 1000;
        if (bytesPerSecond < unit) return (bytesPerSecond < 0 ? 0 : bytesPerSecond) + " B/s";
        char prefix;
        int exp;

        try {
            exp = (int) (Math.log(bytesPerSecond) / Math.log(unit));
            prefix = ("KMGTPE").charAt(exp - 1);
        } catch (IndexOutOfBoundsException e) {
            exp = 6;
            prefix = 'E';
        }

        return String.format("%.1f %cB/s", bytesPerSecond / Math.pow(unit, exp), prefix);
    }

    /**
     * Prepare the download request
     *
     * - initialize `this.connection`
     * - set default connection parameters
     */
    private HttpURLConnection prepareRequest() throws IOException {
        return prepareRequest(this.url);
    }

    private HttpURLConnection prepareRequest(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(30000);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);

        connection.setRequestMethod(this.method.name());
        for (String header: this.header) {
            String[] headerParts = header.split("[ ]*:[ ]*");
            if (headerParts.length != 2) continue;
            connection.setRequestProperty(headerParts[0], headerParts[1]);
        }

        return connection;
    }

    /**
     * Take destination and append next free number with an underscore ('_')
     *
     * @throws IOException
     */
    private void destinationIncrement() throws IOException {
        Path dst = Paths.get(this.basePath, this.destination);
        int currentIncrement = 0;
        String[] fileNameParts = dst.getFileName().toString().split("\\.");
        String fileExtension = fileNameParts.length < 2 ? null : fileNameParts[fileNameParts.length - 1],
                fileName = fileNameParts.length < 2 ? dst.getFileName().toString() : String.join(".",
                        Arrays.copyOf(fileNameParts, fileNameParts.length - 1)
                ),
                fileNamePattern = fileName + "_?([0-9]*)" + (fileExtension == null ? "" : ("\\." + fileExtension));

        DirectoryStream<Path> fileNameCandidates = Files.newDirectoryStream(dst.getParent(),
                file -> file.getFileName().toString().matches(fileNamePattern)
        );

        for (Path candidate: fileNameCandidates) {
            Matcher matcher = Pattern.compile(fileNamePattern).matcher(candidate.getFileName().toString());
            if (matcher.find() && !matcher.group(1).equals("") && Integer.valueOf(matcher.group(1)) > currentIncrement) {
                currentIncrement = Integer.valueOf(matcher.group(1));
            }
        }

        this.destination = fileName + "_" + String.valueOf(currentIncrement + 1) +
                (fileExtension == null ? "" : ("." + fileExtension));
    }

    /**
     * Initialize the destination file
     *
     * - Set destination filename. Check following options in given order:
     *   - From destination specified as part of the request payload
     *   - HTTP header: Content-Disposition
     *   - URL filename
     *   - URL hostname with hyphens ('-') instead of dots ('.') and double-hyphen separated url hash
     * - check if destination is writable
     */
    private void initializeDestination() throws IOException {
        if (this.destination == null) {
            String contentDispositionHeader = this.connection.getHeaderField("Content-Disposition");
            if (contentDispositionHeader != null && contentDispositionHeader.contains("=")) {
                this.destination = contentDispositionHeader.split("=")[1];
                if (this.destination.startsWith("\"") && this.destination.endsWith("\"")) {
                    this.destination = this.destination.substring(1, this.destination.length() - 1);
                }
            } else if (!this.getUrl().getFile().equals("")) {
                String[] urlPath = this.getUrl().getFile().split("/");
                this.destination = urlPath[urlPath.length - 1];
            } else {
                this.destination = this.getUrl().getHost().replace(".", "-") + "--" + this.urlString.hashCode();
            }
        }

        if (Paths.get(this.basePath, this.destination).toFile().exists() && this.keepExisting) {
            destinationIncrement();
        }

        if (!Paths.get(this.basePath, this.destination).getParent().toFile().canWrite()) {
            throw new IOException("Destination path not writable");
        }

        this.file = Paths.get(this.basePath, this.destination).toFile();
    }

    /**
     * Connect to the remote server indicating the given file read offset.
     *
     * @throws IOException On problems with HttpURLConnection or file IO
     * @throws RuntimeException When already running
     */
    private void connect() throws IOException {
        if (this.status == Status.PROGRESSING) {
            throw new RuntimeException("Download is already running");
        }

        this.connection = prepareRequest();
        initializeDestination();

        if (this.file.length() > 0) {
            HttpURLConnection headRequest = prepareRequest();
            headRequest.setRequestMethod("HEAD");
            if (headRequest.getContentLengthLong() > this.file.length()) {
                this.logger.debug("Setting HTTP Range header to resume download '{}'", this.getName());
                this.connection.setRequestProperty("Range", "bytes=" + this.file.length() + "-" + headRequest.getContentLengthLong());
                headRequest.disconnect();
            }
        }

        switch (this.connection.getResponseCode()) {
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
                if (++this.redirectCounter > this.redirectCounterThreshold) {
                    abortDownload();
                    return;
                }
                this.logger.debug("Got {}. redirect\nfor '{}'\nfrom '{}'\n to '{}'", redirectCounter, this.getName(), this.urlString, this.connection.getHeaderField("Location"));
                this.connection.disconnect();
                this.url = new URL(this.connection.getHeaderField("Location"));
                this.connection = prepareRequest();
                connect();
                return;
            case HttpURLConnection.HTTP_OK:
            case HttpURLConnection.HTTP_ACCEPTED:
                this.logger.info("{} ok: Starting download ({})", getName(), getHumanReadableSize());
                this.size = this.connection.getContentLengthLong();

//                Files.copy(this.connection.getInputStream(), this.file.toPath());
                setStatus(Status.PROGRESSING);
                try {
                    byte[] buffer = new byte[8192];
                    while (this.connection.getInputStream().read(buffer, 0, buffer.length) > 0) {
                        Files.write(this.file.toPath(), buffer, this.file.exists() ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
                    }
                    if (this.file.length() >= this.size) {
                        this.logger.info("{} successfully finished ({})", getName(), Paths.get(this.basePath, this.destination).toAbsolutePath().toString());
                        setStatus(Status.SUCCEEDED);
                    }
                } catch (Exception e) {
                    setStatus(Status.FAILED);
                    this.logger.error("Real bad things happened", e);
                }

                this.connection.disconnect();
                break;
            default:
                this.logger.error("Download failed. Server responded with:\n{}\n{}", this.connection.getResponseCode(), this.connection.getResponseMessage());
                throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE,
                        String.format("Cannot start download. Server responded with: (%d) %s", this.connection.getResponseCode(), this.connection.getResponseMessage()));
        }
    }


    /**
     * Interaction
     */

    @JsonIgnore
    public void run() {
        try {
            connect();
        } catch (Exception e) {
            this.status = Status.FAILED;
            this.error = e;
            if (this.connection != null) this.connection.disconnect();
        }
    }

    @JsonIgnore
    public void pauseDownload() {
        try {
            this.logger.info("Pausing download at {}%: '{}'", String.format("%.2f", this.getProgress()*100), this.getName());
            setStatus(Status.PAUSED);
            this.connection.disconnect();
        } catch (Exception e) {
            setStatus(Status.FAILED);
            this.error = e;
            if (this.connection != null) this.connection.disconnect();
        }
    }

    @JsonIgnore
    public void resumeDownload() {
        try {
            this.logger.info("Resuming download at {}%: '{}'", String.format("%.2f", this.getProgress()*100), this.getName());
            connect();
        } catch (Exception e) {
            setStatus(Status.FAILED);
            this.error = e;
            if (this.connection != null) this.connection.disconnect();
        }
    }

    @JsonIgnore
    public void abortDownload() {
        try {
            this.logger.info("Aborting download at {}%: '{}'", String.format("%.2f", this.getProgress()*100), this.getName());
            setStatus(Status.ABORTED);
            this.connection.disconnect();
            if (this.file.exists()) {
                this.file.delete();
            }
        } catch (Exception e) {
            setStatus(Status.FAILED);
            this.error = e;
        }
    }
}
