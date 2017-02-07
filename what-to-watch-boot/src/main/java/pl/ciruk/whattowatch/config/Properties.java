package pl.ciruk.whattowatch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Named;

@Configuration
public class Properties {
    @Value("${zalukaj-login}")
    String zalukajLogin;

    @Bean
    @Named("zalukajLogin")
    String zalukajLogin() {
        return zalukajLogin;
    }

    @Bean
    @Named("zalukajPassword")
    String zalukajPassword() {
        return zalukajPassword;
    }

    @Value("${zalukaj-password}")
    String zalukajPassword;
}
