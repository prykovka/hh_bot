package org.example.reminder;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.example.database.ActivityRepository;
import org.example.menu.MessageTemplates;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ReminderJob implements Job {
    private static final Logger logger = Logger.getLogger(ReminderJob.class.getName());
    private static TelegramBot bot;
    private static ActivityRepository activityRepository;

    public static void setBot(TelegramBot bot) {
        ReminderJob.bot = bot;
    }

    public static void setActivityRepository(ActivityRepository activityRepository) {
        ReminderJob.activityRepository = activityRepository;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        int userId = context.getJobDetail().getJobDataMap().getInt("userId");
        String category = context.getJobDetail().getJobDataMap().getString("category");
        String userName = activityRepository.getUserNameByTgId(userId);

        String messageText = MessageTemplates.getRandomMessage(category, userName);

        if (category.equals("custom")) {
            String customText = activityRepository.getCustomText(userId);
            messageText = String.format("%s, %s", userName, customText);
        }

        SendMessage message = new SendMessage(userId, messageText);

        try {
            bot.execute(message);
            logger.log(Level.INFO, "Message sent successfully to userId={0}", userId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send reminder message to userId=" + userId, e);
        }
    }
}
