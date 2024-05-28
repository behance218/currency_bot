package net.dunice.mk.mkhachemizov.currency_bot.config;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.mkhachemizov.currency_bot.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@RequiredArgsConstructor
public class BotConfiguration {
    private static final Logger log = LoggerFactory.getLogger(BotConfiguration.class);
    private final MessageService messageService;

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(messageService);
        }
        catch (TelegramApiException e) {
            log.error("Не удалось создать бота");
            System.exit(1);
        }
    }
}