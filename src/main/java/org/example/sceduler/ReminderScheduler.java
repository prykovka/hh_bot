package org.example.sceduler;

import org.example.repository.UserRepository;
import org.example.reminder.Reminder;
import org.example.job.ReminderJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.sql.Time;
import java.time.LocalTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс для планирования напоминаний с использованием Quartz Scheduler.
 */
public class ReminderScheduler {
    private static final Logger logger = Logger.getLogger(ReminderScheduler.class.getName());
    private static Scheduler scheduler;

    private ReminderScheduler(){
    }

    static {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            logger.log(Level.SEVERE, "Failed to start scheduler", e);
        }
    }

    /**
     * Планирует напоминание для пользователя.
     *
     * @param userId       ID пользователя.
     * @param category     Категория напоминания.
     * @param activityTime Время напоминания.
     */
    public static void scheduleReminder(long userId, String category, Time activityTime) {
        try {
            JobDetail job = JobBuilder.newJob(ReminderJob.class)
                    .withIdentity("job-" + userId + "-" + category, "group-" + userId)
                    .usingJobData("userId", userId)
                    .usingJobData("category", category)
                    .build();

            LocalTime localTime = activityTime.toLocalTime();
            int hours = localTime.getHour();
            int minutes = localTime.getMinute();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + userId + "-" + category, "group-" + userId)
                    .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(hours, minutes))
                    .build();

            if (scheduler.checkExists(new JobKey("job-" + userId + "-" + category, "group-" + userId))) {
                scheduler.deleteJob(new JobKey("job-" + userId + "-" + category, "group-" + userId));
            }

            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            logger.log(Level.SEVERE, e, () ->  "Ошибка установки напоминания userId=" + userId + ", category=" + category);
        }
    }

    /**
     * Планирует существующие напоминания для всех пользователей.
     *
     * @param userRepository Репозиторий пользователей.
     */
    public static void scheduleExistingReminders(UserRepository userRepository) {
        try {
            for (Reminder reminder : userRepository.getAllReminders()) {
                scheduleReminder(reminder.userId(), reminder.category(), reminder.activityTime());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка получения существующих напоминаний", e);
        }
    }
}
