package net.dunice.mk.mkhachemizov.currency_bot.service;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutdatedCurrencyDeleteScheduler {
    private final CurrencyDeleteService currencyDeleteService;

    @Scheduled(cron = "0 00 10 * * ?") //Every day at 10:00
    public void deleteOutdatedRates() {
        currencyDeleteService.deleteAllCurrencies();
    }

}