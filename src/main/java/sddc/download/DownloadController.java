package sddc.download;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController("DownloadController")
public class DownloadController {

    @Autowired
    private ApplicationContext applicationContext;

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public @ResponseBody List<Download> add(@RequestBody List<DownloadRequest> downloadRequests) {
        return downloadRequests.stream().map(
            download -> {
                Download newDownload = (Download) applicationContext.getBean("newDownload", download);
                newDownload.start();
                return newDownload;
            }
        ).collect(Collectors.toList());
    }
}
