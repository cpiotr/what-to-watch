package pl.ciruk.whattowatch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.ciruk.whattowatch.boot.WhatToWatchBoot;
import pl.ciruk.whattowatch.boot.config.Beans;
import pl.ciruk.whattowatch.boot.config.Bootstrap;
import pl.ciruk.whattowatch.core.suggest.FilmSuggestionProvider;

import java.lang.invoke.MethodHandles;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = WhatToWatchBoot.class)
@ContextConfiguration(classes = WhatToWatchBootTest.TestConfig.class)
@TestPropertySource(locations = "classpath:application-dev.properties")
public class WhatToWatchBootTest {

    @Autowired
    private TestConfig testConfig;

    @Test
    public void contextLoads() {
        assertThat(testConfig).isNotNull();
    }

    @ComponentScan("pl.ciruk.whattowatch")
    @Configuration
    static class TestConfig {
        private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        @Bean
        @Primary
        Bootstrap bootstrap(FilmSuggestionProvider filmSuggestionProvider) {
            return new Bootstrap(filmSuggestionProvider) {
                @Override
                protected void onStartup() {
                    LOGGER.info("Do nothing");
                }
            };
        }
    }
}
