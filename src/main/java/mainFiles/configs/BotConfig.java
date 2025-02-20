package mainFiles.configs;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@PropertySource("application.properties")
public class BotConfig {
    @Value("${bot.name}")
    String botName;

    @Value("${bot.token}")
    String token;

    @Value("${bot.ownerChatId}")
    Long ownerChatId;

    @Value("${bot.channelChatId}")
    Long channelChatId;

    @Value("${bot.checkChannelChatId}")
    boolean checkChannelChatId;

    @Value("${webURL.betboomRegistration}")
    String betboomRegistrationURL;

    @Value("${webURL.site}")
    String siteURL;

    @Value("${imagePath.registration}")
    String registrationImagePath;

    @Value("${imagePath.teams}")
    String teamsImagePath;

    @Value("${user.startBalance}")
    Integer startBalance;
}
