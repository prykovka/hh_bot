package org.example;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import org.example.config.ConfigLoader;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.example.database.UserRepository;
import org.example.database.ActivityRepository;
import org.example.database.CustomReminderRepository;
import org.example.menu.Menu;
import org.example.message.MessageHandler;
import org.example.reminder.ReminderJob;
import org.example.reminder.ReminderScheduler;

import java.sql.Time;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static TelegramBot bot;
    private static ActivityRepository activityRepository;

    public static void main(String[] args) {
        String botToken = ConfigLoader.getProperty("bot.token");

        bot = new TelegramBot(botToken);
        UserRepository userRepository = new UserRepository();
        activityRepository = new ActivityRepository();
        CustomReminderRepository customReminderRepository = new CustomReminderRepository();

        // Set the bot in ReminderJob
        ReminderJob.setBot(bot);

        ReminderScheduler.scheduleExistingReminders(activityRepository, customReminderRepository);

        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (update.message() != null && update.message().text() != null) {
                    MessageHandler.handleIncomingMessage(update);
                } else if (update.callbackQuery() != null) {
                    handleCallbackQuery(update);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        MessageHandler.setBot(bot);
        MessageHandler.setUserRepository(userRepository);
        MessageHandler.setActivityRepository(activityRepository);
        MessageHandler.setCustomReminderRepository(customReminderRepository);
    }

    private static void handleCallbackQuery(Update update) {
        String callbackData = update.callbackQuery().data();
        int chatId = update.callbackQuery().message().chat().id().intValue();
        int messageId = update.callbackQuery().message().messageId();

        if (callbackData.equals("water") || callbackData.equals("exercise") ||
                callbackData.equals("sleep") || callbackData.equals("education") ||
                callbackData.equals("walk") || callbackData.equals("read") || callbackData.equals("custom")) {
            MessageHandler.setCurrentCategory(callbackData);
            if (callbackData.equals("custom")) {
                SendMessage customReminderMessage = new SendMessage(chatId, "О чем вы хотите, чтобы мы вам напомнили? (например, почистить зубы)").replyMarkup(new ForceReply());
                bot.execute(customReminderMessage);
            } else {
                Time existingTime = activityRepository.getActivityTime(chatId, callbackData);
                if (existingTime != null) {
                    InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
                            new InlineKeyboardButton("Нет").callbackData("no_change"));
                    SendMessage message = new SendMessage(chatId, "У вас уже установлено напоминание для " + callbackData + " на " + existingTime.toString() + ". Хотите изменить его время? Пожалуйста, введите новое время в формате HH:MM.")
                            .replyMarkup(inlineKeyboard);
                    bot.execute(message);
                } else {
                    SendMessage requestTimeMessage = new SendMessage(chatId, "Выберите время для напоминания в формате HH:MM (например, 17:30)")
                            .replyMarkup(new ForceReply());
                    bot.execute(requestTimeMessage);
                }
            }
        } else if (callbackData.equals("no_change")) {
            bot.execute(new DeleteMessage(chatId, messageId));
        }
    }
}
