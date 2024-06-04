package org.example;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import org.example.config.ConfigLoader;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import org.example.database.UserRepository;
import org.example.reminder.ReminderJob;
import org.example.reminder.ReminderScheduler;
import org.example.handler.MessageHandler;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static TelegramBot bot;
    private static UserRepository userRepository;

    public static final Map<String, String> categoryTranslations = new HashMap<>();
    static {
        categoryTranslations.put("read", "Чтение");
        categoryTranslations.put("sleep", "Сон");
        categoryTranslations.put("exercise", "Тренировки");
        categoryTranslations.put("water", "Вода");
    }

    public static void main(String[] args) {
        String botToken = ConfigLoader.getProperty("bot.token");
        int adminId = ConfigLoader.getIntProperty("bot.admin");

        bot = new TelegramBot(botToken);
        userRepository = new UserRepository();

        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            ReminderJob.setBot(bot);
            ReminderJob.setUserRepository(userRepository);
        } catch (SchedulerException e) {
            logger.log(Level.SEVERE, "Failed to start scheduler", e);
        }

        MessageHandler.setBot(bot);
        MessageHandler.setUserRepository(userRepository);
        MessageHandler.setAdminId(adminId);

        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (update.message() != null && update.message().text() != null) {
                    MessageHandler.handleIncomingMessage(bot, update);
                } else if (update.callbackQuery() != null) {
                    handleCallbackQuery(update);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        ReminderScheduler.scheduleExistingReminders(userRepository);
    }

    private static void handleCallbackQuery(Update update) {
        String callbackData = update.callbackQuery().data();
        int chatId = update.callbackQuery().message().chat().id().intValue();
        int messageId = update.callbackQuery().message().messageId();

        if (categoryTranslations.containsKey(callbackData)) {
            MessageHandler.setCurrentCategory(callbackData);
            Time existingTime = userRepository.getActivityTime(chatId, callbackData);
            if (existingTime != null) {
                String translatedCategory = categoryTranslations.get(callbackData);
                String formattedTime = new SimpleDateFormat("HH:mm").format(existingTime);
                InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
                        new InlineKeyboardButton("Оставить").callbackData("no_change"),
                        new InlineKeyboardButton("Удалить").callbackData("delete_" + callbackData)
                );
                SendMessage message = new SendMessage(chatId, "У тебя уже установлено напоминание для \"" + translatedCategory + "\" на " + formattedTime + ".\nХотите изменить его время? Пожалуйста, введите новое время в формате HH:MM.")
                        .replyMarkup(inlineKeyboard);
                bot.execute(message);
            } else {
                SendMessage requestTimeMessage = new SendMessage(chatId, "Выбери время в формате HH:MM (например, 17:30)")
                        .replyMarkup(new ForceReply());
                bot.execute(requestTimeMessage);
            }
        } else if (callbackData.equals("no_change")) {
            bot.execute(new DeleteMessage(chatId, messageId));
        } else {
            MessageHandler.handleCallbackQuery(bot, update);
        }
    }
}
