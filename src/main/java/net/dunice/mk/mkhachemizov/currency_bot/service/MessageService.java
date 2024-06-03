package net.dunice.mk.mkhachemizov.currency_bot.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


import lombok.RequiredArgsConstructor;
import net.dunice.mk.mkhachemizov.currency_bot.config.properties.BotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService extends TelegramLongPollingBot {
    private final BotProperties botProperties;
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);
    private final CurrencyUpdaterService currencyUpdaterService;
    private final CurrencyDeleteService currencyDeleteService;
    private final CurrencyConverterService currencyConverterService;
    private final OutdatedCurrencyDeleteScheduler outdatedCurrencyDeleteScheduler;

    private Set<Long> chatIds = new HashSet<>();
    private boolean isAwaitingRubleInput = false;
    private boolean isAwaitingCurrencyInput = false;

    @Override
    public String getBotUsername() {
        return botProperties.name();
    }

    @Override
    public String getBotToken() {
        return botProperties.token();
    }

    @Override
    public void onUpdateReceived(Update update) {
        SendMessage message = messageReceiver(update);
        if (message != null) {
            try {
                execute(message);
            }
            catch (TelegramApiException e) {
                log.error("При ответе юзеру возникла проблема", e);
            }
        }
    }

    public SendMessage messageReceiver(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return handleTextMessage(update);
        }
        else if (update.hasCallbackQuery()) {
            return handleCallbackQuery(update);
        }
        return null;
    }

    private SendMessage handleTextMessage(Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String name = update.getMessage().getChat().getFirstName();

        String responseText;
        ReplyKeyboardMarkup replyMarkup = getMenuKeyboard();

        if (isAwaitingRubleInput) {
            responseText = currencyConverterService.handleRubleCurrencyConversion(text);
            isAwaitingRubleInput = false;
            replyMarkup = getExpandedMenuKeyboard();
        }
        else if (isAwaitingCurrencyInput) {
            responseText = currencyConverterService.handleAnyCurrencyConversion(text);
            isAwaitingCurrencyInput = false;
            replyMarkup = getExpandedMenuKeyboard();
        }
        else {
            switch (text) {
                case "/start" ->
                        responseText = String.format("Привет, %s! Я - твой телеграм бот-конвертер/актуатор валют", name);
                case "/stop" -> responseText = String.format("До свидания, %s! Рад был помочь!", name);
                case "Меню" -> {
                    responseText = "Выберите действие из меню ниже";
                    replyMarkup = getExpandedMenuKeyboard();
                }
                case "Новый курс" -> {
                    responseText = String.format("Понял тебя, %s! Загружаю актуальный курс на сегодня", name);
                    Map<String, Double> rates = currencyUpdaterService.updateCurrencies();
                    responseText += "\nТекущие курсы по отношению к рублю:\n" + rates.entrySet().stream()
                            .map(entry -> "\n" + entry.getKey() + ": " + entry.getValue() + "\n")
                            .collect(Collectors.joining("\n"));
                    replyMarkup = getExpandedMenuKeyboard();
                }
                case "Удалить курс" -> {
                    responseText = String.format("Понял тебя, %s! Удаляю неактуальные данные. Теперь при нажатии на кнопку " +
                            "получения нового курса вы получите самый свежий курс на сегодняшний день", name);
                    currencyDeleteService.deleteAllCurrencies();
                    replyMarkup = getExpandedMenuKeyboard();
                }
                case "Рубль -> Валюта" -> {
                    responseText = String.format("Понял тебя, %s! Введите название целевой валюты и количество рублей для конвертации в виде: \nназвание валюты, количество рублей\n", name);
                    isAwaitingRubleInput = true;
                    replyMarkup = getExpandedMenuKeyboard();
                }
                case "Валюта -> Рубль" -> {
                    responseText = String.format("Понял тебя, %s! Введи название целевой валюты и её количество для конвертации в виде: \nназвание валюты, её количество\n", name);
                    isAwaitingCurrencyInput = true;
                    replyMarkup = getExpandedMenuKeyboard();
                }
                default -> responseText = "Я таких команд не знаю, к сожалению!";
            }
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(responseText);
        message.setReplyMarkup(replyMarkup);
        chatIds.add(chatId);
        log.info("Добавлен chatId: {}", chatId);


        return message;
    }

    private SendMessage handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        String name = callbackQuery.getMessage().getChat().getFirstName();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        String responseText;
        switch (data) {
            case "Старт":
                responseText = String.format("Привет, %s! Я твой телеграм бот-конвертер/актуатор валют", name);
                break;
            case "Новый курс":
                responseText = String.format("Понял тебя, %s. Загружаю актуальный курс на сегодня", name);
                Map<String, Double> rates = currencyUpdaterService.updateCurrencies();
                responseText += "\nТекущие курсы рубля по отношению к следующим валютам:\n" + rates.entrySet().stream()
                        .map(entry -> "\n" + entry.getKey() + ": " + entry.getValue() + "\n")
                        .collect(Collectors.joining("\n"));
                break;
            case "Удалить курс":
                responseText = String.format("Понял тебя, %s. Удаляю устаревшие данные", name);
                currencyDeleteService.deleteAllCurrencies();
                break;
            case "Стоп":
                responseText = String.format("До свидания, %s! Рад был помочь!", name);
                break;
            default:
                responseText = "Я таких команд не знаю, к сожалению!";
                break;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(responseText);
        message.setReplyMarkup(getExpandedMenuKeyboard());
        chatIds.add(chatId);
        log.info("Добавлен chatId: {}", chatId);

        return message;
    }

    private ReplyKeyboardMarkup getMenuKeyboard() {
        KeyboardRow row = new KeyboardRow();
        row.add("Меню");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup getExpandedMenuKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Новый курс");
        row1.add("Удалить курс");
        row1.add("Валюта -> Рубль");
        row1.add("Рубль -> Валюта");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

    public void sendCurrencyUpdateMessage(Map<String, Double> rates) {
        StringBuilder responseText = new StringBuilder("Доброе утро!\nТекущие курсы по отношению к рублю:\n");
        rates.forEach((key, value) -> responseText.append("\n").append(key).append(": ").append(value).append("\n"));

        if (chatIds.isEmpty()) {
            log.warn("Нет сохраненных chatId для отправки сообщений");
        }
        else {
            log.info("Отправка обновлений курсов валют в чаты: {}", chatIds);
        }

        for (Long chatId : chatIds) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(responseText.toString());
            message.setReplyMarkup(getExpandedMenuKeyboard());

            try {
                execute(message);
                log.info("Сообщение с курсами валют отправлено в чат: {}", chatId);
            }
            catch (TelegramApiException e) {
                log.error("Не удалось отправить сообщение в чат: {}", chatId);
            }
        }
    }
}


