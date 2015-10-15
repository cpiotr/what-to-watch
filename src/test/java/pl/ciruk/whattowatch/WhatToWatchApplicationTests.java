package pl.ciruk.whattowatch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.ciruk.whattowatch.config.Application;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = WhatToWatchApplication.class)
@ContextConfiguration(classes = WhatToWatchApplicationTests.TestConfig.class)
public class WhatToWatchApplicationTests {

	@Test
	public void contextLoads() {
	}

	static class TestConfig extends Application {
		@Bean(name = "zalukaj-login-url")
		String loginUrl() {
			return "";
		}

		@Bean(name = "zalukaj-login")
		String login() {
			return "";
		}

		@Bean(name = "zalukaj-password")
		String password() {
			return "";
		}
	}
}
