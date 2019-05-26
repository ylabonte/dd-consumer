package de.yalab.ddc.download;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Request payload for pausing/resuming/aborting/removing downloads.
 */
public class DownloadIds {

    public List<Long> ids;

    public DownloadIds() {
        this.ids = new ArrayList<>();
    }

    public DownloadIds(List<Long> ids) {
        this.ids = ids;
    }

    public DownloadIds(Long id) {
        this.ids = Arrays.asList(id);
    }
}
