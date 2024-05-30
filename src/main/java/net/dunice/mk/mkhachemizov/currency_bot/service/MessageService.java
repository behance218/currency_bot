package net.dunice.mk.mkhachemizov.currency_bot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import lombok.RequiredArgsConstructor;
import net.dunice.mk.mkhachemizov.currency_bot.config.properties.BotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
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
    private final OutdatedCurrencyDeleteService outdatedCurrencyDeleteService;
    //TODO: Deprecated for removal
    private Long lastMessageId = null;
    private Long lastChatId = null;

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
                if (message.getText().contains("Меню") || message.getReplyMarkup() == getExpandedMenuKeyboard()) {
                    lastMessageId = Long.valueOf(message.getReplyToMessageId());
                    lastChatId = Long.valueOf(message.getChatId());
                }
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
            case "Удалить старый курс" -> {
                responseText = String.format("Понял тебя, %s! Удаляю неактуальные данные. Теперь при нажатии на кнопку " +
                        "получения нового курса вы получите самый свежий курс на сегодняшний день", name);
                outdatedCurrencyDeleteService.deleteOutdatedCurrency();
                replyMarkup = getExpandedMenuKeyboard();
            }
            default -> responseText = "Я таких команд не знаю, к сожалению!";
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(responseText);
        message.setReplyMarkup(replyMarkup);

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
            case "Удалить старый курс":
                responseText = String.format("Понял тебя, %s. Удаляю устаревшие данные", name);
                outdatedCurrencyDeleteService.deleteOutdatedCurrency();
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
        //TODO: Deprecated for removal
        lastChatId = chatId;
        lastMessageId = Long.valueOf(messageId);

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
        row1.add("Удалить старый курс");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

    @Deprecated(forRemoval = true)
    private void deletePreviousMessage() {
        if (lastMessageId != null && lastChatId != null) {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(lastChatId.toString());
            deleteMessage.setMessageId(lastMessageId.intValue());
            try {
                execute(deleteMessage);
            }
            catch (TelegramApiException e) {
                log.error("Не удалось удалить предыдущее сообщение", e);
            }
            lastMessageId = null;
            lastChatId = null;
        }
    }
}
