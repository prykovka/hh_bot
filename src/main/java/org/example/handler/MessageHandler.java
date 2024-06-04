package org.example.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.model.Update;
import org.example.Main;
import org.example.database.UserRepository;
import org.example.reminder.ReminderScheduler;
import org.example.templates.menu.Menu;
import org.example.templates.facts.Facts;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageHandler {
    private static final Logger logger = Logger.getLogger(MessageHandler.class.getName());
    private static TelegramBot bot;
    private static UserRepository userRepository;
    private static String currentCategory;
    private static final Map<Integer, Boolean> awaitingFeedback = new HashMap<>();
    private static int adminId;

    public static void setBot(TelegramBot telegramBot) {
        bot = telegramBot;
    }

    public static void setUserRepository(UserRepository repository) {
        userRepository = repository;
    }

    public static void setAdminId(int adminId) {
        MessageHandler.adminId = adminId;
    }

    public static void handleIncomingMessage(TelegramBot bot, Update update) {
        String messageText = update.message().text();
        int chatId = update.message().chat().id().intValue();
        String userName = update.message().chat().username();

        if (awaitingFeedback.getOrDefault(chatId, false)) {
            awaitingFeedback.put(chatId, false);
            SendMessage feedbackMessage = new SendMessage(adminId, "Отзыв от @" + userName + ":\n" + messageText);
            bot.execute(feedbackMessage);
            SendMessage thankYouMessage = new SendMessage(chatId, "Спасибо за твой отзыв! Мне правда важно твое мнение)");
            bot.execute(thankYouMessage);
            return;
        }

        if (messageText.equals("/start")) {
            userRepository.addUser(chatId, userName);
            logger.log(Level.INFO, "Received /start command from user: chatId={0}, userName={1}", new Object[]{chatId, userName});
            SendMessage welcomeMessage = new SendMessage(chatId, "*Здравствуй* \uD83D\uDC4B\n\n" +
                    "/menu - Выбрать и настроить полезные привычки.\n" +
                    "/streak - Все запланированные напоминания и streak.\n" +
                    "/facts - Интересные факты о полезных привычках.\n" +
                    "/feedback - Оставить отзыв.").parseMode(ParseMode.Markdown);
            bot.execute(welcomeMessage);
        } else if (messageText.equals("/menu")) {
            SendMessage menuMessage = new SendMessage(chatId, "Выбери полезное дело:").replyMarkup(Menu.getCategoryMenu());
            bot.execute(menuMessage);
        } else if (messageText.equals("/streak")) {
            handleStreakCommand(chatId);
        } else if (messageText.equals("/facts")) {
            SendMessage factsMenuMessage = new SendMessage(chatId, "Выбери категорию для получения факта:").replyMarkup(Menu.getFactsMenu());
            bot.execute(factsMenuMessage);
        } else if (messageText.equals("/feedback")) {
            awaitingFeedback.put(chatId, true);
            SendMessage feedbackRequestMessage = new SendMessage(chatId, "Пожалуйста, напиши свое искреннее мнение:");
            bot.execute(feedbackRequestMessage);
        } else if (messageText.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$")) {
            Time activityTime = Time.valueOf(messageText + ":00");

            if (currentCategory != null) {
                Time existingTime = userRepository.getActivityTime(chatId, currentCategory);
                String translatedCategory = Main.categoryTranslations.getOrDefault(currentCategory, currentCategory);
                String formattedTime = new SimpleDateFormat("HH:mm").format(activityTime);

                if (existingTime != null) {
                    userRepository.updateActivityTime(chatId, currentCategory, activityTime);
                    ReminderScheduler.scheduleReminder(chatId, currentCategory, activityTime);
                    SendMessage confirmationMessage = new SendMessage(chatId, "Твое время для \"" + translatedCategory + "\" обновлено на " + formattedTime + ".");
                    bot.execute(confirmationMessage);
                } else {
                    userRepository.addActivity(chatId, currentCategory, activityTime);
                    ReminderScheduler.scheduleReminder(chatId, currentCategory, activityTime);
                    SendMessage confirmationMessage = new SendMessage(chatId, "Спасибо! Твое напоминание установлено на " + formattedTime + ".");
                    bot.execute(confirmationMessage);
                }
            } else {
                SendMessage errorMessage = new SendMessage(chatId, "Пожалуйста, выбери категорию привычки.");
                bot.execute(errorMessage);
            }
        } else {
            SendMessage errorMessage = new SendMessage(chatId, "Неверный формат. Пожалуйста, введи время в формате HH:MM.");
            bot.execute(errorMessage);
        }
    }

    private static void handleStreakCommand(int chatId) {
        List<UserRepository.Reminder> reminders = userRepository.getAllRemindersForUser(chatId);
        StringBuilder messageText = new StringBuilder("*Вот твои полезные привычки:*\n\n");

        if (reminders.isEmpty()) {
            messageText.append("У тебя нет запланированных напоминаний:(");
        } else {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            for (UserRepository.Reminder reminder : reminders) {
                String translatedCategory = Main.categoryTranslations.getOrDefault(reminder.getCategory(), reminder.getCategory());
                String formattedTime = timeFormat.format(reminder.getActivityTime());
                int streakNum = userRepository.getStreakNum(chatId, reminder.getCategory());
                messageText.append(translatedCategory).append(" - ").append(formattedTime).append(" - ").append(streakNum).append(" дней\n");
            }
        }

        SendMessage settingsMessage = new SendMessage(chatId, messageText.toString())
                .parseMode(com.pengrad.telegrambot.model.request.ParseMode.Markdown);
        bot.execute(settingsMessage);
    }

    public static void handleCallbackQuery(TelegramBot bot, Update update) {
        String callbackData = update.callbackQuery().data();
        int chatId = update.callbackQuery().message().chat().id().intValue();
        int messageId = update.callbackQuery().message().messageId();

        if (callbackData.startsWith("fact_")) {
            String category = callbackData.replace("fact_", "");
            String fact = Facts.getRandomFact(category);
            SendMessage factMessage = new SendMessage(chatId, fact);
            bot.execute(factMessage);
        } else if (callbackData.startsWith("delete_")) {
            String category = callbackData.replace("delete_", "");
            userRepository.deleteActivity(chatId, category);
            SendMessage deleteConfirmationMessage = new SendMessage(chatId, "Твое напоминание для \"" + Main.categoryTranslations.get(category) + "\" было удалено.");
            bot.execute(deleteConfirmationMessage);
        } else if (callbackData.startsWith("complete_")) {
            String category = callbackData.replace("complete_", "");
            userRepository.incrementStreakNum(chatId, category);
            bot.execute(new DeleteMessage(chatId, messageId));
            int streakNum = userRepository.getStreakNum(chatId, category);
            SendMessage confirmationMessage = new SendMessage(chatId, "Так держать!\n\nТвой streak для \"" + Main.categoryTranslations.get(category) + "\": " + streakNum + " \uD83C\uDF89");
            bot.execute(confirmationMessage);
        } else if (callbackData.startsWith("miss_")) {
            String category = callbackData.replace("miss_", "");
            bot.execute(new DeleteMessage(chatId, messageId));
            int prevstreakNum = userRepository.getStreakNum(chatId, category);
            userRepository.resetStreakNum(chatId, category);
            SendMessage confirmationMessage = new SendMessage(chatId, "Твой streak для \"" + Main.categoryTranslations.get(category) + "\": 0.\n\nА было: " + prevstreakNum + " \uD83D\uDE2D");
            bot.execute(confirmationMessage);
        }
    }

    public static void setCurrentCategory(String category) {
        currentCategory = category;
    }
}