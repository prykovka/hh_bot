package org.example.reminder;

import java.sql.Time;

/**
 * Модель класса, представляющая напоминание.
 */
public class Reminder {
    private final int userId;
    private final String category;
    private final Time activityTime;

    public Reminder(int userId, String category, Time activityTime) {
        this.userId = userId;
        this.category = category;
        this.activityTime = activityTime;
    }

    public int getUserId() {
        return userId;
    }

    public String getCategory() {
        return category;
    }

    public Time getActivityTime() {
        return activityTime;
    }

}
