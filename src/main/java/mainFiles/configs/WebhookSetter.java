package mainFiles.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

@Component
public class WebhookSetter {

    @Value("${bot.token}")
    String botToken;

    @Value("${webURL.site}")
    String siteURL;

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot%s/setWebhook?url=%s/webhook";

    @PostConstruct
    public void setWebhook() {
        String url = String.format(TELEGRAM_API_URL, botToken, siteURL);
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.postForObject(url, null, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}