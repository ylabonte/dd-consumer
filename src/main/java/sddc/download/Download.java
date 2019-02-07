package sddc.download;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.management.relation.InvalidRelationIdException;
import java.io.Serializable;
import java.lang.reflect.Executable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({
    "url",
    "status",
    "message",
    "erroneous",
    "error"
})
public class Download {

    private final static String MESSAGE_OK = "Everything fine! =)";

    private List<String> header = new ArrayList<>();

    private String urlString;

    private URL url = null;

    private String destination = null;

    private Status status = Status.INITIALIZED;

    private Exception error = null;

    public enum Status {
        INITIALIZED,
        WAITING,
        PROGRESSING,
        SUCCEEDED,
        FAILED
    }

    public Download(String url) {
        try {
            this.urlString = url;
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            this.status = Status.FAILED;
            this.error = e;
        }
    }

    public Download(DownloadRequest request) {
        try {
            this.urlString = request.url;
            this.header.addAll(header);
            this.destination = request.destination;
            this.url = new URL(request.url);
        } catch (MalformedURLException e) {
            this.status = Status.FAILED;
            this.error = e;
        }
    }

    public Download setStatus(Status newStatus) {
        this.status = newStatus;
        return this;
    }

    @JsonIgnore
    public URL getUrl() {
        return this.url;
    }

    @JsonProperty("url")
    public String getUrlString() {
        return this.urlString;
    }

    public Status getStatus() {
        return this.status;
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
}
