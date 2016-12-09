package config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Created by jacobhong on 12/8/16.
 */
@Configuration
public class QnaConfig
{
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
