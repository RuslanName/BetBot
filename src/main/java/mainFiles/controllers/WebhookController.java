package mainFiles.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class WebhookController {

    private final TelegramWebhookBot telegramWebhookBot;

    public WebhookController(TelegramWebhookBot telegramWebhookBot) {
        this.telegramWebhookBot = telegramWebhookBot;
    }

    @PostMapping("/webhook")
    public void onWebhookUpdate(@RequestBody Update update) {
        telegramWebhookBot.onWebhookUpdateReceived(update);
    }
}


