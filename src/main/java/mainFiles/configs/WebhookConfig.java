package mainFiles.configs;

import mainFiles.services.TelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebhookConfig {

    @Bean
    public TelegramBot telegramBot(BotConfig botConfig) {
        return new TelegramBot(botConfig);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
