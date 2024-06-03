package net.dunice.mk.mkhachemizov.currency_bot.service;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class CurrencyUpdateScheduler {
    private final CurrencyUpdaterService currencyUpdaterService;
    private final MessageService messageService;

    @Scheduled(cron = "0 1 10 * * ?") //Every day at 10:01
    public void updateRates() {
        Map<String, Double> rates = currencyUpdaterService.updateCurrencies();
        messageService.sendCurrencyUpdateMessage(rates);
    }
}