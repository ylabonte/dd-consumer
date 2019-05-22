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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Scope("prototype")
@ConfigurationProperties(prefix = "download")
@JsonPropertyOrder({
    "url",
    "method",
    "destination",
    "size",
    "status",
    "progress",
    "fileSize",
    "message",
    "erroneous",
    "error"
})
@JsonIgnoreProperties({
    "threadGroup",
    "contextClassLoader",
    "error",
    "stackTrace",
    "allStackTraces",
    "Threads",
    "uncaughtExceptionHandler",
    "defaultUncaughtExceptionHandler",
    "uncaughtExceptionHandler"
})
public class Download extends Thread {

    /**
     * Placeholder message, when no error occurred
     */
    private final static String MESSAGE_OK = "No errors. Everything fine! =)";

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

    /**
     * Download status values
     */
    public enum Status {
        UNINITIALIZED,
        INITIALIZED,
        WAITING,
        PROGRESSING,
        SUCCEEDED,
        FAILED
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
            this.logger.info(String.format("Download initialized:\n- Name: %s\n - Source: %s\n - Destination: %s",
                    getName(),
                    this.urlString,
                    this.destination == null ? "*unspecified*" : this.destination
            ));
//            System.out.println(String.format("Download initialized:\n- Name: %s\n - Source: %s\n - Destination: %s",
//                    getName(),
//                    this.urlString,
//                    this.destination == null ? "*unspecified*" : this.destination
//            ));
        } catch (MalformedURLException e) {
            this.status = Status.FAILED;
            this.error = e;
        }
    }

    /**
     * Set new status
     *
     * @param newStatus
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

    /**
     * Prepare the download request
     *
     * - initialize `this.connection`
     * - set default connection parameters
     */
    private void prepareRequest() throws IOException {
        this.connection = (HttpURLConnection) this.url.openConnection();
        this.connection.setConnectTimeout(5000);
        this.connection.setReadTimeout(30000);
        this.connection.setUseCaches(false);
        this.connection.setInstanceFollowRedirects(true);
        this.connection.setRequestMethod(this.method.name());
        for (String header: this.header) {
            String[] headerParts = header.split("[ ]*:[ ]*");
            if (headerParts.length != 2) continue;
            this.connection.setRequestProperty(headerParts[0], headerParts[1]);
        }
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
     * - set destination based on the url filename
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
    }

    public void run() {
        try {
            setStatus(Status.PROGRESSING);
            prepareRequest();
            initializeDestination();
            if (this.connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                this.logger.info(String.format("%s ok: Starting download (%s)", getName(), getHumanReadableSize()));
//                System.out.println(String.format("%s ok: Starting download (%s)", getName(), getHumanReadableSize()));
                this.size = this.connection.getContentLengthLong();
                Files.copy(this.connection.getInputStream(), Paths.get(this.basePath, this.destination));
                this.logger.info(String.format("%s successfully finished (%s)",
                        getName(),
                        Paths.get(this.basePath, this.destination).toAbsolutePath().toString()
                ));
//                System.out.println(String.format("%s successfully finished (%s)",
//                        getName(),
//                        Paths.get(this.basePath, this.destination).toAbsolutePath().toString()
//                ));
                this.connection.disconnect();
                setStatus(Status.SUCCEEDED);
            } else {
                this.logger.error("Download failed due to HTTP server error response");
                throw new HttpServerErrorException(
                        HttpStatus.valueOf(this.connection.getResponseCode()),
                        this.connection.getResponseMessage()
                );
            }
        } catch (Exception e) {
            this.status = Status.FAILED;
            this.error = e;
            if (this.connection != null) this.connection.disconnect();
        }
    }

    @JsonIgnore
    public void pauseDownload() {
        try {
            this.connection.disconnect();
        } catch (Exception e) {
            this.status = Status.FAILED;
            this.error = e;
            if (this.connection != null) this.connection.disconnect();
        }
    }

    @JsonIgnore
    public void resumeDownload() {
        try {
            HttpURLConnection headRequest = (HttpURLConnection) this.url.openConnection();
            headRequest.setRequestMethod("HEAD");
            if (headRequest.getContentLengthLong() > this.file.length()) {
                this.connection.setRequestProperty("Range", "bytes=" + this.file.length() + "-" + headRequest.getContentLengthLong());
                this.connection.connect();
                //@todo Stream into existing file...
                this.connection.disconnect();
            }
        } catch (Exception e) {
            this.status = Status.FAILED;
            this.error = e;
            if (this.connection != null) this.connection.disconnect();
        }
    }

    public void setBasePath(String basePath) {
        if (this.basePath == null) this.basePath = basePath;
    }

    public void setKeepExisting(Boolean keepExisting) {
        if (this.keepExisting == null) this.keepExisting = keepExisting;
    }

    public String getHumanReadableSize() {
        if (this.connection == null) {
            return "";
        }

        return toHumanReadableSize(this.connection.getContentLengthLong());
//        long bytes = this.connection.getContentLengthLong();
//        int unit = 1024;
//        if (bytes < unit) return bytes + " B";
//        int exp = (int) (Math.log(bytes) / Math.log(unit));
//        char pre = ("KMGTPE").charAt(exp-1);
//        return String.format("%.1f %ciB", bytes / Math.pow(unit, exp), pre);
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public String getHumanReadableFileSize() {
        return toHumanReadableSize(this.fileSize);
    }

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
}
