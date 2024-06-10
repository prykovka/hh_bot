package org.example.callback;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import org.example.repository.UserRepository;
import org.example.handler.MessageHandler;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * Класс для обработки Callback Query в Telegram боте.
 */
public class CallbackQueryHandler {

    private CallbackQueryHandler() {}

    /**
     * Обрабатывает входящий Callback Query.
     *
     * @param bot                  TelegramBot.
     * @param update               Объект обновления, полученный от Telegram.
     * @param userRepository       Репозиторий пользователей для доступа к данным.
     * @param categoryTranslations Категории.
     */
    public static void handleCallbackQuery(TelegramBot bot, Update update, UserRepository userRepository, Map<String, String> categoryTranslations) {
        String callbackData = update.callbackQuery().data();
        if (update.callbackQuery().message() != null && update.callbackQuery().message().chat() != null) {
            long chatId = update.callbackQuery().message().chat().id();
            int messageId = update.callbackQuery().message().messageId();

            if (categoryTranslations.containsKey(callbackData)) {
                handleCategoryCallback(bot, userRepository, categoryTranslations, callbackData, chatId);
            } else if (callbackData.equals("no_change")) {
                bot.execute(new DeleteMessage(chatId, messageId));
            } else {
                MessageHandler.handleCallbackQuery(update);
            }
        } else {
            System.out.println("CallbackQuery does not contain a valid message or chat.");
        }
    }

    /**
     * Обрабатывает Callback Query для выбранной категории.
     *
     * @param bot                  TelegramBot.
     * @param userRepository       Репозиторий пользователей для доступа к данным.
     * @param categoryTranslations Категории.
     * @param callbackData         Данные из Callback Query.
     * @param chatId               Идентификатор чата.
     */
    private static void handleCategoryCallback(TelegramBot bot, UserRepository userRepository, Map<String, String> categoryTranslations, String callbackData, long chatId) {
        MessageHandler.setCurrentCategory(callbackData);
        Time existingTime = userRepository.getActivityTime(chatId, callbackData).orElse(null);
        if (existingTime != null) {
            sendExistingReminderMessage(bot, categoryTranslations, callbackData, chatId, existingTime);
        } else {
            sendTimeRequestMessage(bot, chatId);
        }
    }

    /**
     * Отправляет сообщение с существующим напоминанием.
     *
     * @param bot                  TelegramBot.
     * @param categoryTranslations Категории.
     * @param callbackData         Данные из Callback Query.
     * @param chatId               Идентификатор чата.
     * @param existingTime         Существующее время напоминания.
     */
    private static void sendExistingReminderMessage(TelegramBot bot, Map<String, String> categoryTranslations, String callbackData, long chatId, Time existingTime) {
        String translatedCategory = categoryTranslations.get(callbackData);
        String formattedTime = new SimpleDateFormat("HH:mm").format(existingTime);
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("Оставить").callbackData("no_change"),
                new InlineKeyboardButton("Удалить").callbackData("delete_" + callbackData)
        );
        SendMessage message = new SendMessage(chatId, "У тебя уже установлено напоминание для \"" + translatedCategory + "\" на " + formattedTime + ".\nХотите изменить его время? Пожалуйста, введите новое время в формате HH:MM.")
                .replyMarkup(inlineKeyboard);
        bot.execute(message);
    }

    /**
     * Отправляет сообщение с запросом времени.
     *
     * @param bot    TelegramBot.
     * @param chatId Идентификатор чата.
     */
    private static void sendTimeRequestMessage(TelegramBot bot, long chatId) {
        SendMessage requestTimeMessage = new SendMessage(chatId, "Выбери время в формате HH:MM (например, 17:30)")
                .replyMarkup(new ForceReply());
        bot.execute(requestTimeMessage);
    }
}
