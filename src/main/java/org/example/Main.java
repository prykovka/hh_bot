package org.example;

import com.pengrad.telegrambot.TelegramBot;
import org.example.config.ConfigLoader;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.example.repository.UserRepository;
import org.example.callback.CallbackQueryHandler;
import org.example.handler.MessageHandler;
import org.example.job.ReminderJob;
import org.example.sceduler.ReminderScheduler;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Главный класс приложения, запускающий Telegram-бота и планировщик напоминаний.
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    /**
     * Категории.
     */
    public static final Map<String, String> categoryTranslations = new HashMap<>();
    static {
        categoryTranslations.put("read", "Чтение");
        categoryTranslations.put("sleep", "Сон");
        categoryTranslations.put("exercise", "Тренировки");
        categoryTranslations.put("water", "Вода");
    }

    /**
     * Основной метод запуска приложения.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        UserRepository userRepository;
        TelegramBot bot;
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
                    CallbackQueryHandler.handleCallbackQuery(bot, update, userRepository, categoryTranslations);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        ReminderScheduler.scheduleExistingReminders(userRepository);

    }
}