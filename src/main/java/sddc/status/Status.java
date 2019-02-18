package sddc.status;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import sddc.download.Download;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Status class holds a list of all downloads as part of the application sddc.status
 * and therefore implements a thread safe singleton pattern.
 */
@JsonPropertyOrder({
    "downloads",
    "waitingDownloads",
    "progressingDownloads",
    "succeededDownloads",
    "failedDownloads",
    "currentDownloads",
    "downloadHistory"
})
public class Status {

    private static Status instance;

    private final ArrayList<Download> downloads;

    private Status() {
        this.downloads = new ArrayList<>();
        try {
//            this.downloads.add(new Download("http://www.example.com", "/tmp"));
//            this.downloads.add(new Download("https://gist.githubusercontent.com/ylabonte/79d36b4f17635d7661bcac75677cd216/raw/7139d45328fe2300595beb946eb4d3c5647df2b7/DownloadRequests.schema.json", "/tmp"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @JsonIgnore
    public static synchronized Status getInstance() {
        if (Status.instance == null) {
            Status.instance = new Status();
        }
        return Status.instance;
    }

    public List<Download> getDownloads() {
        return this.downloads;
    }

    public List<Download> getWaitingDownloads() {
        return this.downloads.stream().filter(download ->
            download.getStatus().equals(Download.Status.WAITING)
        ).collect(Collectors.toList());
    }

    public List<Download> getProgressingDownloads() {
        return this.downloads.stream().filter(download ->
            download.getStatus().equals(Download.Status.PROGRESSING)
        ).collect(Collectors.toList());
    }

    public List<Download> getSucceededDownloads() {
        return this.downloads.stream().filter(download ->
            download.getStatus().equals(Download.Status.SUCCEEDED)
        ).collect(Collectors.toList());
    }

    public List<Download> getFailedDownloads() {
        return this.downloads.stream().filter(download ->
            download.getStatus().equals(Download.Status.FAILED)
        ).collect(Collectors.toList());
    }

    public List<Download> getCurrentDownloads() {
        return this.downloads.stream().filter(download ->
            download.getStatus().compareTo(Download.Status.INITIALIZED) > 0 &&
            download.getStatus().compareTo(Download.Status.SUCCEEDED) < 0
        ).collect(Collectors.toList());
    }

    public List<Download> getDownloadHistory() {
        return this.downloads.stream().filter(download ->
            download.getStatus().compareTo(Download.Status.SUCCEEDED) >= 0
        ).collect(Collectors.toList());
    }
}
