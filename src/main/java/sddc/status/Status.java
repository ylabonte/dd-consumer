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
            Download initialized =  new Download("http://www.example.com"),
                     waiting = new Download("http://www.example.com"),
                     progressing = new Download("http://www.example.com"),
                     succeeded = new Download("http://www.example.com"),
                     failed = new Download("http://www.example.com");
            waiting.setStatus(Download.Status.WAITING);
            progressing.setStatus(Download.Status.PROGRESSING);
            succeeded.setStatus(Download.Status.SUCCEEDED);
            failed.setStatus(Download.Status.FAILED);
            this.downloads.add(initialized);
            this.downloads.add(waiting);
            this.downloads.add(progressing);
            this.downloads.add(failed);
            this.downloads.add(succeeded);
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
