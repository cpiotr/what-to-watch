package pl.ciruk.whattowatch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = WhatToWatchBoot.class)
@ContextConfiguration(classes = WhatToWatchBootTests.TestConfig.class)
@TestPropertySource(locations="classpath:application-dev.properties")
public class WhatToWatchBootTests {

	@Test
	public void contextLoads() {
	    // Check if application context finds all beans and dependencies
	}

	@ComponentScan("pl.ciruk.whattowatch")
	static class TestConfig {
		// Nothing to override
	}
}
