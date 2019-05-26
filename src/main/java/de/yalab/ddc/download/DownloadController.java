package de.yalab.ddc.download;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import de.yalab.ddc.status.Status;
import org.springframework.web.server.ServerErrorException;

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

    @RequestMapping(value = "/remove", method = RequestMethod.POST)
    public ResponseEntity remove(@RequestBody DownloadIds downloadIds) {
        try {
            for (long id : downloadIds.ids) {
                Status.getInstance().removeDownload(id);
            }
            return new ResponseEntity(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            throw new ServerErrorException(e.getMessage(), e);
        }
    }

    @RequestMapping(value = "/abort", method = RequestMethod.POST)
    public ResponseEntity abort(@RequestBody DownloadIds downloadIds) {
        try {
            for (long id : downloadIds.ids) {
                Status.getInstance().getDownload(id).abortDownload();
            }
            return new ResponseEntity(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            throw new ServerErrorException(e.getMessage(), e);
        }
    }

    @RequestMapping(value = "/pause", method = RequestMethod.POST)
    public ResponseEntity pause(@RequestBody DownloadIds downloadIds) {
        try {
            for (long id : downloadIds.ids) {
                Status.getInstance().getDownload(id).pauseDownload();
            }
            return new ResponseEntity(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            throw new ServerErrorException(e.getMessage(), e);
        }
    }

    @RequestMapping(value = "/resume", method = RequestMethod.POST)
    public ResponseEntity resume(@RequestBody DownloadIds downloadIds) {
        try {
            for (long id : downloadIds.ids) {
                Status.getInstance().getDownload(id).resumeDownload();
            }
            return new ResponseEntity(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            throw new ServerErrorException(e.getMessage(), e);
        }
    }
}
