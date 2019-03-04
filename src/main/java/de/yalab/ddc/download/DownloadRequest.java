package de.yalab.ddc.download;

import java.util.ArrayList;
import java.util.List;

public class DownloadRequest {

    public List<String> header = new ArrayList<>();

    public String url = null;

    public String method = "GET";

    public String destination = null;
}
