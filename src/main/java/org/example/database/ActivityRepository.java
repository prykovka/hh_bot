package org.example.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActivityRepository {
    private static final Logger logger = Logger.getLogger(ActivityRepository.class.getName());

    public void addActivity(int tgId, String category, Time activityTime) {
        String query = "INSERT INTO activities (user_id, category, activity_time) VALUES ((SELECT id FROM Users WHERE tg_id = ?), ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, tgId);
            statement.setString(2, category);
            statement.setTime(3, activityTime);
            statement.executeUpdate();

            logger.log(Level.INFO, "Activity added successfully: tgId={0}, category={1}, activityTime={2}", new Object[]{tgId, category, activityTime});

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding activity: tgId=" + tgId + ", category=" + category + ", activityTime=" + activityTime, e);
        }
    }

    public Time getActivityTime(int tgId, String category) {
        String query = "SELECT activity_time FROM activities a JOIN Users u ON a.user_id = u.id WHERE u.tg_id = ? AND a.category = ?";
        Time activityTime = null;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, tgId);
            statement.setString(2, category);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                activityTime = resultSet.getTime("activity_time");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving activity time for userId=" + tgId + ", category=" + category, e);
        }

        return activityTime;
    }

    public String getUserNameByTgId(int tgId) {
        String query = "SELECT user_name FROM Users WHERE tg_id = ?";
        String userName = null;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, tgId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                userName = resultSet.getString("user_name");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving user name by tgId: " + tgId, e);
        }

        return userName;
    }

    public List<Reminder> getAllReminders() {
        String query = "SELECT u.tg_id, a.category, a.activity_time FROM activities a JOIN Users u ON a.user_id = u.id";
        List<Reminder> reminders = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int tgId = resultSet.getInt("tg_id");
                String category = resultSet.getString("category");
                Time activityTime = resultSet.getTime("activity_time");

                reminders.add(new Reminder(tgId, category, activityTime));
            }

            logger.log(Level.INFO, "Retrieved {0} reminders from the database", reminders.size());

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving reminders", e);
        }

        return reminders;
    }

    public static class Reminder {
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
}
