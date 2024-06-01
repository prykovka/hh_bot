package org.example.reminder;

import org.example.database.ActivityRepository;
import org.example.database.CustomReminderRepository;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.sql.Time;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReminderScheduler {
    private static final Logger logger = Logger.getLogger(ReminderScheduler.class.getName());
    private static Scheduler scheduler;

    static {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            logger.log(Level.SEVERE, "Failed to start scheduler", e);
        }
    }

    public static void scheduleReminder(int userId, String category, Time activityTime) {
        try {
            JobDetail job = JobBuilder.newJob(ReminderJob.class)
                    .withIdentity("job-" + userId + "-" + category, "group-" + userId)
                    .usingJobData("userId", userId)
                    .usingJobData("category", category)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + userId + "-" + category, "group-" + userId)
                    .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(activityTime.getHours(), activityTime.getMinutes()))
                    .build();

            if (scheduler.checkExists(new JobKey("job-" + userId + "-" + category, "group-" + userId))) {
                scheduler.deleteJob(new JobKey("job-" + userId + "-" + category, "group-" + userId));
            }

            scheduler.scheduleJob(job, trigger);
            logger.log(Level.INFO, "Scheduled reminder: userId={0}, category={1}, activityTime={2}", new Object[]{userId, category, activityTime});
        } catch (SchedulerException e) {
            logger.log(Level.SEVERE, "Failed to schedule reminder for userId=" + userId + ", category=" + category, e);
        }
    }

    public static void scheduleExistingReminders(ActivityRepository activityRepository, CustomReminderRepository customReminderRepository) {
        try {
            for (ActivityRepository.Reminder reminder : activityRepository.getAllReminders()) {
                String reminderCategory = reminder.getCategory();
                scheduleReminder(reminder.getUserId(), reminderCategory, reminder.getActivityTime());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to schedule existing reminders", e);
        }
    }
}
