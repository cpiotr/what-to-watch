package pl.ciruk.whattowatch.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.lang.invoke.MethodHandles;

@SpringBootApplication
@SuppressWarnings("PMD")
public class WhatToWatchBoot implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("${server.port:8080}")
    Integer serverPort;

    public static void main(String[] args) {
        SpringApplication.run(WhatToWatchBoot.class, args);

    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOGGER.info("Open the application: http://{}:{}/events.html", "localhost", serverPort);
    }
}
