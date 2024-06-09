package org.example.reminder;

import java.sql.Time;

/**
 * Модель класса, представляющая напоминание.
 */
public record Reminder(long userId, String category, Time activityTime) {

}
