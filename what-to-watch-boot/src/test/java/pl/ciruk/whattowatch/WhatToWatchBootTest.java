package pl.ciruk.whattowatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pl.ciruk.whattowatch.boot.WhatToWatchBoot;
import pl.ciruk.whattowatch.boot.config.Bootstrap;
import pl.ciruk.whattowatch.core.filter.FilmFilter;
import pl.ciruk.whattowatch.core.suggest.FilmSuggestionProvider;

import java.lang.invoke.MethodHandles;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WhatToWatchBoot.class)
@ContextConfiguration(classes = WhatToWatchBootTest.InternalConfig.class)
class WhatToWatchBootTest {

    @Autowired
    private InternalConfig internalConfig;

    @Test
    void contextLoads() {
        assertThat(internalConfig).isNotNull();
    }

    @ComponentScan("pl.ciruk.whattowatch")
    @Configuration
    static class InternalConfig {
        private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        @Bean
        @Primary
        Bootstrap bootstrap(FilmSuggestionProvider filmSuggestionProvider, FilmFilter filmFilter) {
            return new Bootstrap(filmSuggestionProvider, filmFilter) {
                @Override
                protected void onStartup() {
                    LOGGER.info("Do nothing");
                }
            };
        }
    }
}
