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
import java.util.Optional;

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
        int chatId = update.callbackQuery().message().chat().id().intValue();
        int messageId = update.callbackQuery().message().messageId();

        if (categoryTranslations.containsKey(callbackData)) {
            handleCategoryCallback(bot, userRepository, categoryTranslations, callbackData, chatId, messageId);
        } else if (callbackData.equals("no_change")) {
            bot.execute(new DeleteMessage(chatId, messageId));
        } else {
            MessageHandler.handleCallbackQuery(update);
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
     * @param messageId            Идентификатор сообщения.
     */
    private static void handleCategoryCallback(TelegramBot bot, UserRepository userRepository, Map<String, String> categoryTranslations, String callbackData, int chatId, int messageId) {
        MessageHandler.setCurrentCategory(callbackData);
        Optional<Time> existingTime = userRepository.getActivityTime(chatId, callbackData);
        if (existingTime.isPresent()) {
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
    private static void sendExistingReminderMessage(TelegramBot bot, Map<String, String> categoryTranslations, String callbackData, int chatId, Optional<Time> existingTime) {
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
    private static void sendTimeRequestMessage(TelegramBot bot, int chatId) {
        SendMessage requestTimeMessage = new SendMessage(chatId, "Выбери время в формате HH:MM (например, 17:30)")
                .replyMarkup(new ForceReply());
        bot.execute(requestTimeMessage);
    }
}
