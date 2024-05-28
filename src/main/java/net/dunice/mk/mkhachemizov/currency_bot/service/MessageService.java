package net.dunice.mk.mkhachemizov.currency_bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final CurrencyUpdaterService currencyUpdaterService;
    private final OutdatedCurrencyDeleteService outdatedCurrencyDeleteService;

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
            responseText = String.format("Привет, %s! Я - твой телеграм бот-конвертер/актуатор валют", name);
        }
        else if (text.equals("/stop")) {
            responseText = String.format("До свидания, %s! Рад был помочь!", name);
        }
        else {
            responseText = "Я таких команд не знаю, к сожалению!";
        }

        var message = new SendMessage();
        message.setChatId(chatId);
        message.setText(responseText);
        message.setReplyMarkup(getInlineKeyboard());

        return message;
    }

    private SendMessage handleCallbackQuery(Update update) {
        var callbackQuery = update.getCallbackQuery();
        var data = callbackQuery.getData();
        var chatId = callbackQuery.getMessage().getChatId();
        var name = callbackQuery.getMessage().getChat().getFirstName();

        String responseText;
        switch (data) {
            case "Старт" -> responseText = String.format("Привет, %s! Я твой телеграм бот-конвертер/актуатор валют", name);
            case "Новый курс" -> {
                responseText = String.format("Понял тебя, %s. Загружаю актуальный курс на сегодня", name);
                currencyUpdaterService.updateCurrencies();
            }
            case "Удалить старый курс" -> {
                responseText = String.format("Понял тебя, %s. Удаляю устаревшие данные", name);
                outdatedCurrencyDeleteService.deleteOutdatedCurrency();
            }
            case "Стоп" -> responseText = String.format("До свидания, %s! Рад был помочь!", name);
            default -> responseText = "Я таких команд не знаю, к сожалению!";
        }


        var message = new SendMessage();
        message.setChatId(chatId);
        message.setText(responseText);

        return message;
    }

    private InlineKeyboardMarkup getInlineKeyboard() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("Новый курс", "Новый курс"));
        row1.add(createButton("Стоп", "stop"));
        row1.add((createButton("Удалить старый курс", "Удалить старый курс")));
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
}