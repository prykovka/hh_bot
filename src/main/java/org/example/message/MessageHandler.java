package org.example.message;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.model.Update;
import org.example.Main;
import org.example.database.UserRepository;
import org.example.reminder.ReminderScheduler;
import org.example.menu.Menu;
import org.example.facts.Facts;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.List;
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
        } else if (messageText.equals("/settings")) {
            handleSettingsCommand(chatId);
        } else if (messageText.equals("/facts")) {
            SendMessage factsMenuMessage = new SendMessage(chatId, "Выберите категорию для получения факта:").replyMarkup(Menu.getFactsMenu());
            bot.execute(factsMenuMessage);
        } else if (messageText.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$")) {
            Time activityTime = Time.valueOf(messageText + ":00");

            if (currentCategory != null) {
                Time existingTime = userRepository.getActivityTime(chatId, currentCategory);
                String translatedCategory = Main.categoryTranslations.getOrDefault(currentCategory, currentCategory);
                String formattedTime = new SimpleDateFormat("HH:mm").format(activityTime);

                if (existingTime != null) {
                    userRepository.updateActivityTime(chatId, currentCategory, activityTime);
                    ReminderScheduler.scheduleReminder(chatId, currentCategory, activityTime);
                    SendMessage confirmationMessage = new SendMessage(chatId, "Ваше время для \"" + translatedCategory + "\" обновлено на " + formattedTime + ".");
                    bot.execute(confirmationMessage);
                } else {
                    userRepository.addActivity(chatId, currentCategory, activityTime);
                    ReminderScheduler.scheduleReminder(chatId, currentCategory, activityTime);
                    SendMessage confirmationMessage = new SendMessage(chatId, "Спасибо! Ваше напоминание установлено на " + formattedTime + ".");
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

    private static void handleSettingsCommand(int chatId) {
        List<UserRepository.Reminder> reminders = userRepository.getAllRemindersForUser(chatId);
        StringBuilder messageText = new StringBuilder("Вот ваши напоминания:\n");

        if (reminders.isEmpty()) {
            messageText.append("У вас нет запланированных напоминаний.");
        } else {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            for (UserRepository.Reminder reminder : reminders) {
                String translatedCategory = Main.categoryTranslations.getOrDefault(reminder.getCategory(), reminder.getCategory());
                String formattedTime = timeFormat.format(reminder.getActivityTime());
                messageText.append(translatedCategory).append(" - ").append(formattedTime).append("\n");
            }
        }

        SendMessage settingsMessage = new SendMessage(chatId, messageText.toString());
        bot.execute(settingsMessage);
    }

    public static void handleCallbackQuery(TelegramBot bot, Update update) {
        String callbackData = update.callbackQuery().data();
        int chatId = update.callbackQuery().message().chat().id().intValue();

        if (callbackData.startsWith("fact_")) {
            String category = callbackData.replace("fact_", "");
            String fact = Facts.getRandomFact(category);
            SendMessage factMessage = new SendMessage(chatId, fact);
            bot.execute(factMessage);
        }
    }

    public static void setCurrentCategory(String category) {
        currentCategory = category;
    }
}
