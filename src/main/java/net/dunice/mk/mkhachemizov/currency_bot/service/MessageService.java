package net.dunice.mk.mkhachemizov.currency_bot.service;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.mkhachemizov.currency_bot.config.properties.BotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
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

    @Override
    public String getBotUsername() {
        return botProperties.name();
    }

    @Override
    public String getBotToken() {
        return botProperties.token();
    }

    //TODO: ДОПИЛИТЬ ЭТОТ МЕТОД, ВОЗМОЖНО СНОВА РАЗДЕЛИТЬ MESSAGE SERVICE ОТ TELEGRAMLONGPOLLINGBOT-а, перенести как раньше отдельно
    @Override
    public void onUpdateReceived(Update update) {
//        SendMessage message = messageReceiver(update);
//        if (message != null) {
//            try {
//                execute(message);
//                if (message.getText().contains("Меню") || message.getReplyMarkup() == getExpandedMenuKeyboard()) {
//                    lastMessageId = message.getMessageId;
//                    lastChatId = Long.valueOf(message.getChatId());
//                }
//            } catch (TelegramApiException e) {
//                log.error("При ответе юзеру возникла проблема", e);
//            }
////            sendMessageAndSaveId(message);
//        }
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

        deletePreviousMessage();

        String responseText;
        ReplyKeyboardMarkup replyMarkup = getMenuKeyboard();

        if (text.equals("/start")) {
            responseText = String.format("Привет, %s! Я - твой телеграм бот-конвертер/актуатор валют", name);
        }
        else if (text.equals("/stop")) {
            responseText = String.format("До свидания, %s! Рад был помочь!", name);
        }
        else if (text.equals("Меню")) {
            responseText = "Выберите действие из меню ниже";
            replyMarkup = getExpandedMenuKeyboard();
        }
        else if (text.equals("Новый курс")) {
            responseText = String.format("Понял тебя, %s! Загружаю актуальный курс на сегодня", name);
            Map<String, Double> rates = currencyUpdaterService.updateCurrencies();
            responseText += "\nТекущие курсы по отношению к рублю:\n" + rates.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining("\n"));
            replyMarkup = getExpandedMenuKeyboard();
        }
        else if (text.equals("Удалить старый курс")) {
            responseText = String.format("Понял тебя, %s! Удаляю неактуальные данные. Теперь при нажатии на кнопку " +
                    "получения нового курса вы получите самый свежий курс на сегодняшний день", name);
            outdatedCurrencyDeleteService.deleteOutdatedCurrency();
            replyMarkup = getExpandedMenuKeyboard();
        }
        else {
            responseText = "Я таких команд не знаю, к сожалению!";
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

        deletePreviousMessage();

        String responseText;
        switch (data) {
            case "Старт":
                responseText = String.format("Привет, %s! Я твой телеграм бот-конвертер/актуатор валют", name);
                break;
            case "Новый курс":
                responseText = String.format("Понял тебя, %s. Загружаю актуальный курс на сегодня", name);
                Map<String, Double> rates = currencyUpdaterService.updateCurrencies();
                responseText += "\nТекущие курсы по отношению к рублю:\n" + rates.entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
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

//    private InlineKeyboardMarkup getInlineKeyboard() {
//        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
//
//        List<InlineKeyboardButton> row1 = new ArrayList<>();
//        row1.add(createButton("Новый курс", "Новый курс"));
//        row1.add(createButton("Стоп", "stop"));
//        row1.add(createButton("Удалить старый курс", "Удалить старый курс"));
//        keyboard.add(row1);
//        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
//        inlineKeyboardMarkup.setKeyboard(keyboard);
//        return inlineKeyboardMarkup;
//    }

//    private InlineKeyboardButton createButton(String text, String callbackData) {
//        InlineKeyboardButton button = new InlineKeyboardButton();
//        button.setText(text);
//        button.setCallbackData(callbackData);
//        return button;
//    }

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

    private void sendMessageAndSaveId(SendMessage message) {
        try {
            Message sentMessage = execute(message);
            lastMessageId = Long.valueOf(sentMessage.getMessageId());
            lastChatId = sentMessage.getChatId();
        }
        catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение или получить ID", e);
        }
    }
}
