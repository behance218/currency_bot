package net.dunice.mk.mkhachemizov.currency_bot.service;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.mkhachemizov.currency_bot.repository.TelegramBotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OutdatedCurrencyDeleteService {

    private static final Logger log = LoggerFactory.getLogger(OutdatedCurrencyDeleteService.class);
    private final TelegramBotRepository telegramBotRepository;

    public void deleteOutdatedCurrency() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            telegramBotRepository.deleteOutdatedCurrency(yesterday);
        } catch (Exception e) {
            log.error("Не удалось удалить устаревшие даты из базы");
        }
    }
}