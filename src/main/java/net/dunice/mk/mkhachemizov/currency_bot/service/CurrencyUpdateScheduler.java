package net.dunice.mk.mkhachemizov.currency_bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrencyUpdateScheduler {
    private final CurrencyUpdaterService currencyUpdaterService;

    @Scheduled(cron = "0 30 8 * * ?") //Every day at 8:30
    public void updateRates() {
        currencyUpdaterService.updateCurrencies();
    }
}