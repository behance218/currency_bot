package net.dunice.mk.mkhachemizov.currency_bot.service;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.mkhachemizov.currency_bot.config.properties.BotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService extends TelegramLongPollingBot {
    private final BotProperties botProperties;
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);
    private final CurrencyUpdaterService currencyUpdaterService;
    private final OutdatedCurrencyDeleteService outdatedCurrencyDeleteService;
    private Long lastMessageId = null;
    private Long lastChatId = null;

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
        var text = update.getMessage().getText();
        var chatId = update.getMessage().getChatId();
        var name = update.getMessage().getChat().getFirstName();

        String responseText;
        if (text.equals("/start")) {
            deletePreviousMessage();
            responseText = String.format("Привет, %s! Я - твой телеграм бот-конвертер/актуатор валют", name);
        }
        else if (text.equals("/stop")) {
            deletePreviousMessage();
            responseText = String.format("До свидания, %s! Рад был помочь!", name);
        }
        else {
            deletePreviousMessage();
            responseText = "Я таких команд не знаю, к сожалению!";
        }

        var message = new SendMessage();
        message.setChatId(chatId);
        message.setText(responseText);
        message.setReplyMarkup(getInlineKeyboard());
        deletePreviousMessage();
        return message;
    }

    private SendMessage handleCallbackQuery(Update update) {
        var callbackQuery = update.getCallbackQuery();
        var data = callbackQuery.getData();
        var chatId = callbackQuery.getMessage().getChatId();
        var name = callbackQuery.getMessage().getChat().getFirstName();

        String responseText;
        switch (data) {
            case "Старт" ->
                    responseText = String.format("Привет, %s! Я твой телеграм бот-конвертер/актуатор валют", name);
            case "Новый курс" -> {
                responseText = String.format("Понял тебя, %s. Загружаю актуальный курс на сегодня", name);
                Map<String, Double> rates = currencyUpdaterService.updateCurrencies();
                responseText += "\nТекущие курсы по отношению к рублю:\n" + rates.entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.joining("\n"));
            }
            case "Удалить старый курс" -> {
                responseText = String.format("Понял тебя, %s. Удаляю устаревшие данные", name);
                deletePreviousMessage();
                outdatedCurrencyDeleteService.deleteOutdatedCurrency();
            }
            case "Стоп" -> responseText = String.format("До свидания, %s! Рад был помочь!", name);
            default -> responseText = "Я таких команд не знаю, к сожалению!";
        }

        var message = new SendMessage();
        message.setChatId(chatId);
        message.setText(responseText);
//        deletePreviousMessage();

        return message;
    }

    private InlineKeyboardMarkup getInlineKeyboard() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("Новый курс", "Новый курс"));
        row1.add(createButton("Стоп", "stop"));
        row1.add(createButton("Удалить старый курс", "Удалить старый курс"));
        keyboard.add(row1);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private void deletePreviousMessage() {
        if (lastMessageId != null && lastChatId != null) {
            deleteMessage(lastChatId, lastMessageId);
            lastMessageId = null;
            lastChatId = null;
        }
    }

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
        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            log.error("При ответе юзеру возникла проблема", e);
        }
    }

    public void deleteMessage(Long chatId, Long messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId.intValue());
        try {
            execute(deleteMessage);
        }
        catch (TelegramApiException e) {
            log.error("Не удалось удалить сообщение", e);
        }
    }
}