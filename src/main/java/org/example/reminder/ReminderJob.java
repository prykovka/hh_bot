package org.example.reminder;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.example.database.UserRepository;
import org.example.templates.messages.MessagesTemplates;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ReminderJob implements Job {
    private static final Logger logger = Logger.getLogger(ReminderJob.class.getName());
    private static TelegramBot bot;
    private static UserRepository userRepository;

    public static void setBot(TelegramBot bot) {
        ReminderJob.bot = bot;
    }

    public static void setUserRepository(UserRepository userRepository) {
        ReminderJob.userRepository = userRepository;
    }

    @Override
    public void execute(JobExecutionContext context) {
        int userId = context.getJobDetail().getJobDataMap().getInt("userId");
        String category = context.getJobDetail().getJobDataMap().getString("category");
        String userName = userRepository.getUserNameByTgId(userId);

        String messageText = MessagesTemplates.getRandomMessage(category, userName);

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("✅").callbackData("complete_" + category),
                new InlineKeyboardButton("❌").callbackData("miss_" + category)
        );

        SendMessage message = new SendMessage(userId, messageText)
                .replyMarkup(inlineKeyboard);

        try {
            bot.execute(message);
            logger.log(Level.INFO, "Message sent successfully to userId={0}", userId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send reminder message to userId=" + userId, e);
        }
    }
}
