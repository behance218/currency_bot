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

    public Map<String, Double> updateCurrencies() {
        Map<String, Double> rates = new HashMap<>();
        try {
            String url = "https://www.cbr.ru/currency_base/daily/";
            Document doc = Jsoup.connect(url).get();
            Elements rows = doc.select("table.data tr");

            for (Element row : rows) {
                Elements columns = row.select("td");
                if (columns.size() > 4) {
                    String unitStr = columns.get(2).text();
                    String currencyName = columns.get(3).text();
                    double rate = Double.parseDouble(columns.get(4).text().replace(",", "."));
                    if (!unitStr.equals("1")){
                        int unit = Integer.parseInt(unitStr);
                        rate /= unit;
                    }
                    rates.put(currencyName, rate);
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
        return rates;
    }
}
