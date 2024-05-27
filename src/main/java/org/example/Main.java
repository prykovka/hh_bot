package org.example;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.example.config.ConfigLoader;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import org.example.database.UserRepository;

import java.sql.Time;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static TelegramBot bot;
    private static UserRepository userRepository;

    private static String currentCategory;

    public static void main(String[] args) {
        String botToken = ConfigLoader.getProperty("bot.token");

        bot = new TelegramBot(botToken); // Инициализация статической переменной bot
        userRepository = new UserRepository();

        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (update.message() != null && update.message().text() != null) {
                    handleIncomingMessage(update);
                } else if (update.callbackQuery() != null) {
                    handleCallbackQuery(update);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private static void handleIncomingMessage(Update update) {
        String messageText = update.message().text();
        int chatId = update.message().chat().id().intValue();
        String userName = update.message().chat().username();

        if (messageText.equals("/start")) {
            userRepository.addUser(chatId, userName);
            logger.log(Level.INFO, "Received /start command from user: chatId={0}, userName={1}", new Object[]{chatId, userName});
            SendMessage welcomeMessage = new SendMessage(chatId, "Добро пожаловать! Используйте команду /menu для выбора привычек.");
            bot.execute(welcomeMessage);
        } else if (messageText.equals("/menu")) {
            InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
                    new InlineKeyboardButton[]{
                            new InlineKeyboardButton("Вода").callbackData("water"),
                            new InlineKeyboardButton("Спорт").callbackData("exercise")
                    },
                    new InlineKeyboardButton[]{
                            new InlineKeyboardButton("Сон").callbackData("sleep"),
                            new InlineKeyboardButton("Обучение").callbackData("education")
                    },
                    new InlineKeyboardButton[]{
                            new InlineKeyboardButton("Прогулки").callbackData("walk"),
                            new InlineKeyboardButton("Чтение").callbackData("read")
                    }
            );

            SendMessage menuMessage = new SendMessage(chatId, "Выберите привычку:")
                    .replyMarkup(inlineKeyboard);
            bot.execute(menuMessage);
        } else if (messageText.matches("\\d{2}:\\d{2}")) {
            // Обработка ответа пользователя с временем напоминания
            Time activityTime = Time.valueOf(messageText + ":00");
            userRepository.addActivity(chatId, currentCategory, activityTime);
            SendMessage confirmationMessage = new SendMessage(chatId, "Спасибо! Ваше напоминание установлено на " + messageText + ".");
            bot.execute(confirmationMessage);
        } else {
            SendMessage errorMessage = new SendMessage(chatId, "Неверный формат. Пожалуйста, введите время в формате HH:MM.");
            bot.execute(errorMessage);
        }
    }

    private static void handleCallbackQuery(Update update) {
        String callbackData = update.callbackQuery().data();
        int chatId = update.callbackQuery().message().chat().id().intValue();

        // Обработка callback data
        if (callbackData.equals("water") || callbackData.equals("exercise") ||
                callbackData.equals("sleep") || callbackData.equals("education") ||
                callbackData.equals("walk") || callbackData.equals("read")) {
            currentCategory = callbackData;
            SendMessage requestTimeMessage = new SendMessage(chatId, "Выберите время для напоминания в формате HH:MM (например, 17:30)")
                    .replyMarkup(new ForceReply());
            bot.execute(requestTimeMessage);
        }
    }
}
