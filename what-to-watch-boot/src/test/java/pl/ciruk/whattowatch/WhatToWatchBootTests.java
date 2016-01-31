package pl.ciruk.whattowatch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = WhatToWatchBoot.class)
@ContextConfiguration(classes = WhatToWatchBootTests.TestConfig.class)
@TestPropertySource(locations="classpath:application-dev.properties")
public class WhatToWatchBootTests {

	@Test
	public void contextLoads() {
	}

	@ComponentScan("pl.ciruk")
	static class TestConfig {
		// Nothing to override
	}
}
