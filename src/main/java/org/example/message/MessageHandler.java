package org.example.message;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.model.Update;
import org.example.database.UserRepository;
import org.example.reminder.ReminderScheduler;
import org.example.menu.Menu;

import java.sql.Time;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageHandler {
    private static final Logger logger = Logger.getLogger(MessageHandler.class.getName());
    private static TelegramBot bot;
    private static UserRepository userRepository;
    private static String currentCategory;

    public static void setBot(TelegramBot telegramBot) {
        bot = telegramBot;
    }

    public static void setUserRepository(UserRepository repository) {
        userRepository = repository;
    }

    public static void handleIncomingMessage(TelegramBot bot, Update update) {
        String messageText = update.message().text();
        int chatId = update.message().chat().id().intValue();
        String userName = update.message().chat().username();

        if (messageText.equals("/start")) {
            userRepository.addUser(chatId, userName);
            logger.log(Level.INFO, "Received /start command from user: chatId={0}, userName={1}", new Object[]{chatId, userName});
            SendMessage welcomeMessage = new SendMessage(chatId, "Добро пожаловать! Используйте команду /menu для выбора привычек.");
            bot.execute(welcomeMessage);
        } else if (messageText.equals("/menu")) {
            SendMessage menuMessage = new SendMessage(chatId, "Выберите привычку:").replyMarkup(Menu.getCategoryMenu());
            bot.execute(menuMessage);
        } else if (messageText.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$")) {
            Time activityTime = Time.valueOf(messageText + ":00");

            if (currentCategory != null) {
                Time existingTime = userRepository.getActivityTime(chatId, currentCategory);
                if (existingTime != null) {
                    userRepository.updateActivityTime(chatId, currentCategory, activityTime);
                    ReminderScheduler.scheduleReminder(chatId, currentCategory, activityTime);
                    SendMessage confirmationMessage = new SendMessage(chatId, "Ваше время напоминания для " + currentCategory + " обновлено на " + messageText + ".");
                    bot.execute(confirmationMessage);
                } else {
                    userRepository.addActivity(chatId, currentCategory, activityTime);
                    ReminderScheduler.scheduleReminder(chatId, currentCategory, activityTime);
                    SendMessage confirmationMessage = new SendMessage(chatId, "Спасибо! Ваше напоминание установлено на " + messageText + ".");
                    bot.execute(confirmationMessage);
                }
            } else {
                SendMessage errorMessage = new SendMessage(chatId, "Пожалуйста, выберите категорию привычки.");
                bot.execute(errorMessage);
            }
        } else {
            SendMessage errorMessage = new SendMessage(chatId, "Неверный формат. Пожалуйста, введите время в формате HH:MM.");
            bot.execute(errorMessage);
        }
    }

    public static void setCurrentCategory(String category) {
        currentCategory = category;
    }
}
