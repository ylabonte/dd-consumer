package sddc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import sddc.download.Download;

@SpringBootApplication
@EnableConfigurationProperties(Download.class)
public class Application {
    /**
     * Application entry point
     *
     * @param args Spring boot command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}