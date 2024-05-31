package net.dunice.mk.mkhachemizov.currency_bot.service;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.mkhachemizov.currency_bot.entity.CurrencyRate;
import net.dunice.mk.mkhachemizov.currency_bot.repository.TelegramBotRepository;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrencyConverterService {
    private final TelegramBotRepository telegramBotRepository;

    public String handleRubleCurrencyConversion(String userInput) {
        String[] inputParts = userInput.split(",");
        if (inputParts.length != 2) {
            return "Неверный формат ввода. Пожалуйста, введи данные в формате: название валюты, количество рублей";
        }
        String currencyName = inputParts[0].trim();
        String rublesStr = inputParts[1].trim();
        double rubles;

        try {
            rubles = Double.parseDouble(rublesStr);
        }
        catch (NumberFormatException e) {
            return "Количество рублей должно быть числом. Попробуй снова";
        }
        CurrencyRate currencyRate = telegramBotRepository.findByName(currencyName);
        if (currencyRate != null) {
            double rate = currencyRate.getRate();
            double convertedAmount = rubles / rate;
            return String.format("%.2f руб = %.4f %s", rubles, convertedAmount, currencyRate.getName());
        }
        else {
            return "Я не могу найти указанную валюту";
        }
    }

    public String handleAnyCurrencyConversion(String userInput) {
        String[] inputParts = userInput.split(",");
        if (inputParts.length != 2) {
            return "Неверный формат ввода. Пожалуйста, введи данные в формате: название валюты, её количество";
        }
        String currencyName = inputParts[0].trim();
        String currencyStr = inputParts[1].trim();
        double rate;

        try {
            rate = Double.parseDouble(currencyStr);
        }
        catch (NumberFormatException e) {
            return "Количество исходной валюты должно быть числом";
        }
        CurrencyRate currencyRate = telegramBotRepository.findByName(currencyName);
        if (currencyRate != null) {
            double rubleRate = currencyRate.getRate();
            double convertedRubles = rate * rubleRate;
            return String.format("%.2f %s = %.2f Рублей", rate, currencyRate.getName(), convertedRubles);
        }
        else {
            return "Я не могу найти указанную валюту";
        }
    }
}
