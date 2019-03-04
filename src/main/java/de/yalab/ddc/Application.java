package de.yalab.ddc;

import de.yalab.ddc.download.Download;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

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