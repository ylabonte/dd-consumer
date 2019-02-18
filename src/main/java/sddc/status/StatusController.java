package sddc.status;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public @ResponseBody Status status() {
        return Status.getInstance();
    }
}
