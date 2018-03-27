package pl.ciruk.whattowatch;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.ciruk.whattowatch.config.Beans;
import pl.ciruk.whattowatch.config.Bootstrap;
import pl.ciruk.whattowatch.suggest.FilmSuggestionProvider;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = WhatToWatchBoot.class)
@ContextConfiguration(classes = WhatToWatchBootTest.TestConfig.class)
@TestPropertySource(locations = "classpath:application-dev.properties")
public class WhatToWatchBootTest {

    @Test
    public void contextLoads() {
        // Check if application context finds all beans and dependencies
    }

    @ComponentScan("pl.ciruk.whattowatch")
    @Configuration
    @Import(Beans.class)
    @Slf4j
    static class TestConfig {
        @Bean
        @Primary
        Bootstrap bootstrap(FilmSuggestionProvider filmSuggestionProvider) {
            return new Bootstrap(filmSuggestionProvider) {
                @Override
                protected void onStartup() {
                    log.info("Do nothing");
                }
            };
        }
    }
}
