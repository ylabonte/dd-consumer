package de.yalab.ddc.download;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import de.yalab.ddc.status.Status;

import java.util.List;
import java.util.stream.Collectors;

@RestController("DownloadController")
public class DownloadController {

    @Autowired
    private ApplicationContext applicationContext;

    private Logger logger = LoggerFactory.getLogger(DownloadController.class);

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public @ResponseBody List<Download> add(@RequestBody List<DownloadRequest> downloadRequests) {
        return downloadRequests.stream().map(
            request -> {
                Download download = (Download) applicationContext.getBean("download", request);
                Status.getInstance().addDownload(download);
                download.start();
                return download;
            }
        ).collect(Collectors.toList());
    }
}
