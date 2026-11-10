package pl.ciruk.whattowatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pl.ciruk.whattowatch.boot.WhatToWatchBoot;
import pl.ciruk.whattowatch.boot.config.Bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WhatToWatchBoot.class, properties = "w2w.bootstrap.enabled=false")
@ContextConfiguration(classes = WhatToWatchBootTest.InternalConfig.class)
class WhatToWatchBootTest {

    @Autowired
    private InternalConfig internalConfig;

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(internalConfig).isNotNull();
    }

    @Test
    void shouldNotWarmUpCachesWhenDisabled() {
        assertThat(context.getBeansOfType(Bootstrap.class)).isEmpty();
    }

    @ComponentScan("pl.ciruk.whattowatch")
    @Configuration
    static class InternalConfig {
    }
}
