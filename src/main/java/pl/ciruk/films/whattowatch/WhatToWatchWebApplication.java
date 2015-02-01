package pl.ciruk.films.whattowatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan("pl.ciruk")
public class WhatToWatchWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhatToWatchWebApplication.class, args);
    }
}
