package org.example.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import org.example.Main;
import org.example.repository.UserRepository;
import org.example.reminder.Reminder;
import org.example.sceduler.ReminderScheduler;
import org.example.templates.facts.Facts;
import org.example.templates.menu.Menu;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс для обработки отправляемых сообщений и команд.
 */
public class MessageHandler {
    private static final Logger logger = Logger.getLogger(MessageHandler.class.getName());
    private static TelegramBot bot;
    private static UserRepository userRepository;
    private static String currentCategory;
    private static final Map<Long, Boolean> awaitingFeedback = new HashMap<>();
    private static final Map<Long, Boolean> awaitingName = new HashMap<>();
    private static int adminId;

    private MessageHandler() {
        // Приватный конструктор для предотвращения инстанцирования
    }

    // Геттеры и сеттеры для приватных полей

    /**
     * Устанавливает объект Telegram бота.
     *
     * @param telegramBot объект Telegram бота.
     */
    public static void setBot(TelegramBot telegramBot) {
        bot = telegramBot;
    }

    /**
     * Устанавливает репозиторий пользователей.
     *
     * @param repository репозиторий пользователей.
     */
    public static void setUserRepository(UserRepository repository) {
        userRepository = repository;
    }

    /**
     * Устанавливает ID администратора.
     *
     * @param adminId ID администратора.
     */
    public static void setAdminId(int adminId) {
        MessageHandler.adminId = adminId;
    }

    /**
     * Обрабатывает входящие сообщения от Telegram бота.
     *
     * @param update объект обновления Telegram.
     */
    public static void handleIncomingMessage(Update update) {
        String messageText = update.message().text();
        long chatId = update.message().chat().id().intValue();
        String userName = update.message().chat().username();

        if (Boolean.TRUE.equals(awaitingFeedback.getOrDefault(chatId, Boolean.FALSE))) {
            handleFeedback(chatId, userName, messageText);
            return;
        }

        if (Boolean.TRUE.equals(awaitingName.getOrDefault(chatId, Boolean.FALSE))) {
            handleNameUpdate(chatId, messageText);
            return;
        }

        switch (messageText) {
            case "/start" -> handleStartCommand(chatId, userName);
            case "/menu" -> handleMenuCommand(chatId);
            case "/streak" -> handleStreakCommand(chatId);
            case "/facts" -> handleFactsCommand(chatId);
            case "/feedback" -> handleFeedbackCommand(chatId);
            default -> handleDefaultCommand(chatId, messageText);
        }
    }

    /**
     * Обрабатывает отзыв пользователя.
     *
     * @param chatId     ID чата.
     * @param userName   Имя пользователя.
     * @param messageText Текст сообщения.
     */
    private static void handleFeedback(long chatId, String userName, String messageText) {
        awaitingFeedback.put(chatId, false);
        SendMessage feedbackMessage = new SendMessage(adminId, "Отзыв от @" + userName + ":\n" + messageText);
        bot.execute(feedbackMessage);
        SendMessage thankYouMessage = new SendMessage(chatId, "Спасибо за твой отзыв! Мне правда важно мнение человеков)");
        bot.execute(thankYouMessage);
    }

    /**
     * Обрабатывает команду /start.
     *
     * @param chatId   ID чата.
     * @param userName Имя пользователя.
     */
    private static void handleStartCommand(long chatId, String userName) {
        userRepository.addUser(chatId, userName);
        logger.log(Level.INFO, "Получена /start от: chatId={0}, userName={1}", new Object[]{chatId, userName});
        String welcomeText = """
        *Здравствуй* \uD83D\uDC4B

        /menu - Выбрать и настроить полезные привычки.
        /streak - Все запланированные напоминания и streak.
        /facts - Интересные факты о полезных привычках.
        /feedback - Оставить отзыв.
        """;
        SendMessage welcomeMessage = new SendMessage(chatId, welcomeText).parseMode(ParseMode.Markdown);
        bot.execute(welcomeMessage);
    }

    /**
     * Обрабатывает команду /menu.
     *
     * @param chatId ID чата.
     */
    private static void handleMenuCommand(long chatId) {
        SendMessage menuMessage = new SendMessage(chatId, "Выбери полезное дело:").replyMarkup(Menu.getCategoryMenu());
        bot.execute(menuMessage);
    }

    /**
     * Обрабатывает команду /streak.
     *
     * @param chatId ID чата.
     */
    private static void handleStreakCommand(long chatId) {
        List<Reminder> reminders = userRepository.getAllRemindersForUser(chatId);
        StringBuilder messageText = new StringBuilder("*Вот твои полезные привычки:*\n\n");

        if (reminders.isEmpty()) {
            messageText.append("У тебя нет запланированных напоминаний:(");
        } else {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            for (Reminder reminder : reminders) {
                String translatedCategory = Main.categoryTranslations.getOrDefault(reminder.category(), reminder.category());
                String formattedTime = timeFormat.format(reminder.activityTime());
                int streakNum = userRepository.getStreakNum(chatId, reminder.category());
                messageText.append(translatedCategory).append(" - ").append(formattedTime).append(" - ").append(streakNum).append(" дней\n");
            }
        }

        SendMessage settingsMessage = new SendMessage(chatId, messageText.toString())
                .parseMode(ParseMode.Markdown);
        bot.execute(settingsMessage);
    }

    /**
     * Обрабатывает команду /facts.
     *
     * @param chatId ID чата.
     */
    private static void handleFactsCommand(long chatId) {
        SendMessage factsMenuMessage = new SendMessage(chatId, "Выбери категорию для получения факта:").replyMarkup(Menu.getFactsMenu());
        bot.execute(factsMenuMessage);
    }

    /**
     * Обрабатывает команду /feedback.
     *
     * @param chatId ID чата.
     */
    private static void handleFeedbackCommand(long chatId) {
        awaitingFeedback.put(chatId, true);
        SendMessage feedbackRequestMessage = new SendMessage(chatId, "Пожалуйста, напиши свое искреннее мнение:");
        bot.execute(feedbackRequestMessage);
    }

    /**
     * Обрабатывает неверный формат времени.
     *
     * @param chatId     ID чата.
     * @param messageText Текст сообщения.
     */
    private static void handleDefaultCommand(long chatId, String messageText) {
        if (messageText.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$")) {
            Time activityTime = Time.valueOf(messageText + ":00");
            handleTimeInput(chatId, activityTime);
        } else {
            SendMessage errorMessage = new SendMessage(chatId, "Неверный формат. Пожалуйста, введи время в формате HH:MM.");
            bot.execute(errorMessage);
        }
    }

    /**
     * Обрабатывает ввод времени.
     *
     * @param chatId       ID чата.
     * @param activityTime Время действия.
     */
    private static void handleTimeInput(long chatId, Time activityTime) {
        if (currentCategory != null) {
            Optional<Time> existingTime = userRepository.getActivityTime(chatId, currentCategory);
            String translatedCategory = Main.categoryTranslations.getOrDefault(currentCategory, currentCategory);
            String formattedTime = new SimpleDateFormat("HH:mm").format(activityTime);

            if (existingTime.isPresent()) {
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
    }

    /**
     * Обрабатывает входящие запросы.
     *
     * @param update объект обновления Telegram.
     */
    public static void handleCallbackQuery(Update update) {
        if (update.callbackQuery() != null && update.message() != null && update.message().chat() != null) {
            String callbackData = update.callbackQuery().data();
            long chatId = update.message().chat().id();
            int messageId = update.message().messageId();

            if (callbackData.startsWith("fact_")) {
                handleFactCallback(chatId, callbackData);
            } else if (callbackData.startsWith("delete_")) {
                handleDeleteCallback(chatId, callbackData);
            } else if (callbackData.startsWith("complete_")) {
                handleCompleteCallback(chatId, callbackData, messageId);
            } else if (callbackData.startsWith("miss_")) {
                handleMissCallback(chatId, callbackData, messageId);
            } else if (callbackData.equals("yes_name")) {
                SendMessage message = new SendMessage(chatId, "Как мне тебя называть?").replyMarkup(new ForceReply());
                awaitingName.put(chatId, true);
                bot.execute(message);
            }
        } else {
            logger.log(Level.WARNING, "CallbackQuery does not contain a valid message or chat.");
        }
    }

    private static void handleFactCallback(long chatId, String callbackData) {
        String category = callbackData.replace("fact_", "");
        String fact = Facts.getRandomFact(category);
        SendMessage factMessage = new SendMessage(chatId, fact);
        bot.execute(factMessage);
    }

    private static void handleDeleteCallback(long chatId, String callbackData) {
        String category = callbackData.replace("delete_", "");
        userRepository.deleteActivity(chatId, category);
        SendMessage deleteConfirmationMessage = new SendMessage(chatId, "Твое напоминание для \"" + Main.categoryTranslations.get(category) + "\" было удалено.");
        bot.execute(deleteConfirmationMessage);
    }

    private static void handleCompleteCallback(long chatId, String callbackData, int messageId) {
        String category = callbackData.replace("complete_", "");
        userRepository.incrementStreakNum(chatId, category);
        bot.execute(new DeleteMessage(chatId, messageId));
        int streakNum = userRepository.getStreakNum(chatId, category);
        SendMessage confirmationMessage = new SendMessage(chatId, "Так держать!\n\nТвой streak для \"" + Main.categoryTranslations.get(category) + "\": " + streakNum + " \uD83C\uDF89");
        bot.execute(confirmationMessage);
    }

    private static void handleMissCallback(long chatId, String callbackData, int messageId) {
        String category = callbackData.replace("miss_", "");
        bot.execute(new DeleteMessage(chatId, messageId));
        int prevStreakNum = userRepository.getStreakNum(chatId, category);
        userRepository.resetStreakNum(chatId, category);
        SendMessage confirmationMessage = new SendMessage(chatId, "Твой streak для \"" + Main.categoryTranslations.get(category) + "\": 0.\n\nА было: " + prevStreakNum + " \uD83D\uDE2D");
        bot.execute(confirmationMessage);
    }

    /**
     * Устанавливает текущую категорию.
     *
     * @param category Текущая категория.
     */
    public static void setCurrentCategory(String category) {
        currentCategory = category;
    }

    /**
     * Обрабатывает обновление имени пользователя.
     *
     * @param chatId  ID чата.
     * @param newName Новое имя пользователя.
     */
    private static void handleNameUpdate(long chatId, String newName) {
        awaitingName.put(chatId, false);
        userRepository.updateUserName(chatId, newName);
        SendMessage thankYouMessage = new SendMessage(chatId, "Приятно познакомиться, " + newName + "!\nВот немного обо мне:\n/menu - Выбрать и настроить полезные привычки.\n/streak - Все запланированные напоминания и streak.\n/facts - Интересные факты о полезных привычках.\n/feedback - Оставить отзыв.");
        bot.execute(thankYouMessage);
    }
}
