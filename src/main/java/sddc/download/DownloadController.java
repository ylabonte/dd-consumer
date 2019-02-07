package sddc.download;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class DownloadController {
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public @ResponseBody List<Download> add(@RequestBody List<DownloadRequest> downloadRequests) {
        return downloadRequests.stream().map(download -> new Download(download)).collect(Collectors.toList());
    }
}
