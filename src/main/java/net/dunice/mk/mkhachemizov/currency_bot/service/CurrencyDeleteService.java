package net.dunice.mk.mkhachemizov.currency_bot.service;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.mkhachemizov.currency_bot.repository.TelegramBotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CurrencyDeleteService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyDeleteService.class);
    private final TelegramBotRepository telegramBotRepository;

    public void deleteAllCurrencies() {
        try {
            telegramBotRepository.deleteCurrencies();
        }
        catch (Exception e) {
            log.error("Не удалось удалить устаревшие даты из базы");
        }
    }
}