package net.dunice.mk.mkhachemizov.currency_bot.service;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.mkhachemizov.currency_bot.entity.CurrencyRate;
import net.dunice.mk.mkhachemizov.currency_bot.repository.TelegramBotRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurrencyUpdaterService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyUpdaterService.class);
    private final TelegramBotRepository repository;

    public void updateCurrencies() {
        try {
            String url = "https://www.cbr.ru/currency_base/daily/";
            Document doc = Jsoup.connect(url).get();
            Elements rows = doc.select("table.data tr");

            Map<String, Double> rates = new HashMap<>();

            for (Element row : rows) {
                Elements columns = row.select("td");
                if (columns.size() > 4) {
                    String currencyCode = columns.get(1).text();
                    Double rate = Double.parseDouble(columns.get(4).text().replace(",", "."));
                    rates.put(currencyCode, rate);
                }
            }
            for (String key : rates.keySet()) {
                CurrencyRate rate = new CurrencyRate();
                rate.setName(key);
                rate.setDate(LocalDate.now());
                rate.setRate(rates.get(key));
                repository.save(rate);
            }
        }
        catch (Exception e) {
            log.error("Не удалось вытянуть актуальный курс", e);
        }
    }

}
