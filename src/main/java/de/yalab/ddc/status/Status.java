package de.yalab.ddc.status;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.yalab.ddc.download.Download;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Status class holds a list of all downloads as part of the application DownloadService.status
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

    private final List<Download> downloads;

    private final Map<Long, Download> _downloads;

    private Status() {
        this.downloads = new ArrayList<>();
        this._downloads = new HashMap<>();
    }

    @JsonIgnore
    public static synchronized Status getInstance() {
        if (Status.instance == null) {
            Status.instance = new Status();
        }
        return Status.instance;
    }

    public void addDownload(Download download) {
        this.downloads.add(download);
        this._downloads.put(download.getId(), download);
    }

    public List<Download> getDownloads() {
        return this.downloads;
    }

    @JsonIgnore
    public List<Download> getWaitingDownloads() {
        return this.downloads.stream().filter(download ->
            download.getStatus().equals(Download.Status.WAITING)
        ).collect(Collectors.toList());
    }

    @JsonIgnore
    public List<Download> getProgressingDownloads() {
        return this.downloads.stream().filter(download ->
            download.getStatus().equals(Download.Status.PROGRESSING)
        ).collect(Collectors.toList());
    }

    @JsonIgnore
    public List<Download> getSucceededDownloads() {
        return this.downloads.stream().filter(download ->
            download.getStatus().equals(Download.Status.SUCCEEDED)
        ).collect(Collectors.toList());
    }

    @JsonIgnore
    public List<Download> getFailedDownloads() {
        return this.downloads.stream().filter(download ->
            download.getStatus().equals(Download.Status.FAILED)
        ).collect(Collectors.toList());
    }

    @JsonIgnore
    public List<Download> getCurrentDownloads() {
        return this.downloads.stream().filter(download ->
            download.getStatus().compareTo(Download.Status.INITIALIZED) > 0 &&
            download.getStatus().compareTo(Download.Status.SUCCEEDED) < 0
        ).collect(Collectors.toList());
    }

    @JsonIgnore
    public List<Download> getDownloadHistory() {
        return this.downloads.stream().filter(download ->
            download.getStatus().compareTo(Download.Status.SUCCEEDED) >= 0
        ).collect(Collectors.toList());
    }

    @JsonIgnore
    public Download getDownload(long id) {
        return this._downloads.get(id);
    }

    @JsonIgnore
    public void removeDownload(long id) {
        Download download = this._downloads.get(id);
        download.pauseDownload();
        this.downloads.remove(download);
        this._downloads.remove(id);
    }
}
