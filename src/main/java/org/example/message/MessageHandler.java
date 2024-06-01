package org.example.message;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ForceReply;
import org.example.database.CustomReminderRepository;
import org.example.database.UserRepository;
import org.example.database.ActivityRepository;
import org.example.reminder.ReminderScheduler;
import org.example.menu.Menu;

import java.sql.Time;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageHandler {
    private static final Logger logger = Logger.getLogger(MessageHandler.class.getName());
    private static TelegramBot bot;
    private static UserRepository userRepository;
    private static ActivityRepository activityRepository;
    private static CustomReminderRepository customReminderRepository;
    private static String currentCategory;
    private static String customText;
    private static boolean waitingForCustomText = false;
    private static boolean waitingForTime = false;

    public static void setBot(TelegramBot telegramBot) {
        bot = telegramBot;
    }

    public static void setUserRepository(UserRepository repository) {
        userRepository = repository;
    }

    public static void setActivityRepository(ActivityRepository repository) {
        activityRepository = repository;
    }

    public static void setCustomReminderRepository(CustomReminderRepository repository) {
        customReminderRepository = repository;
    }

    public static void handleIncomingMessage(Update update) {
        String messageText = update.message().text();
        int chatId = update.message().chat().id().intValue();
        String userName = update.message().chat().username();

        logger.log(Level.INFO, "Received message: {0}", messageText);

        if (messageText.equals("/start")) {
            userRepository.addUser(chatId, userName);
            logger.log(Level.INFO, "Received /start command from user: chatId={0}, userName={1}", new Object[]{chatId, userName});
            SendMessage welcomeMessage = new SendMessage(chatId, "Добро пожаловать! Используйте команду /menu для выбора привычек.");
            bot.execute(welcomeMessage);
        } else if (messageText.equals("/menu")) {
            waitingForCustomText = false;
            waitingForTime = false;
            SendMessage menuMessage = new SendMessage(chatId, "Выберите привычку:").replyMarkup(Menu.getCategoryMenu());
            bot.execute(menuMessage);
        } else if (waitingForCustomText) {
            customText = messageText;
            customReminderRepository.addCustomReminder(chatId, customText);
            waitingForCustomText = false;
            waitingForTime = true;
            SendMessage requestTimeMessage = new SendMessage(chatId, "Когда вам напомнить об этом? Введите время в формате HH:MM.")
                    .replyMarkup(new ForceReply());
            bot.execute(requestTimeMessage);
        } else if (waitingForTime) {
            if (messageText.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$")) {
                Time activityTime = Time.valueOf(messageText + ":00");
                waitingForTime = false;

                if (currentCategory.equals("custom")) {
                    activityRepository.addActivity(chatId, "custom", activityTime);
                    ReminderScheduler.scheduleReminder(chatId, customText, activityTime);
                    SendMessage confirmationMessage = new SendMessage(chatId, "Спасибо! Ваше кастомное напоминание установлено на " + messageText + ".");
                    bot.execute(confirmationMessage);
                } else {
                    activityRepository.addActivity(chatId, currentCategory, activityTime);
                    ReminderScheduler.scheduleReminder(chatId, currentCategory, activityTime);
                    SendMessage confirmationMessage = new SendMessage(chatId, "Спасибо! Ваше напоминание установлено на " + messageText + ".");
                    bot.execute(confirmationMessage);
                }
            } else {
                SendMessage errorMessage = new SendMessage(chatId, "Неверный формат. Пожалуйста, введите время в формате HH:MM.")
                        .replyMarkup(new ForceReply());
                bot.execute(errorMessage);
            }
        } else {
            SendMessage errorMessage = new SendMessage(chatId, "Неверный формат. Пожалуйста, используйте команду /menu для выбора привычки или введите время в формате HH:MM.")
                    .replyMarkup(new ForceReply());
            bot.execute(errorMessage);
        }
    }

    public static void setCurrentCategory(String category) {
        currentCategory = category;
        if (category.equals("custom")) {
            waitingForCustomText = true;
        } else {
            waitingForTime = true;
        }
    }
}
