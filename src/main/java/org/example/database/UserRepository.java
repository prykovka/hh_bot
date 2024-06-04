package org.example.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserRepository {
    private static final Logger logger = Logger.getLogger(UserRepository.class.getName());

    public void addUser(int tgId, String userName) {
        String query = "INSERT INTO Users (tg_id, user_name) VALUES (?, ?) ON CONFLICT (tg_id) DO NOTHING";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, tgId);
            statement.setString(2, userName);
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                logger.log(Level.INFO, "User added successfully: tgId={0}, userName={1}", new Object[]{tgId, userName});
            } else {
                logger.log(Level.INFO, "User already exists: tgId={0}", tgId);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding user: tgId=" + tgId + ", userName=" + userName, e);
        }
    }

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

    public String getUserNameByTgId(int tgId) {
        String query = "SELECT user_name FROM Users WHERE tg_id = ?";
        String userName = null;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, tgId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                userName = resultSet.getString("user_name");
                logger.log(Level.INFO, "User found: tgId={0}, userName={1}", new Object[]{tgId, userName});
            } else {
                logger.log(Level.INFO, "User not found: tgId={0}", tgId);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving user: tgId=" + tgId, e);
        }

        return userName;
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

    public void updateActivityTime(int tgId, String category, Time newTime) {
        String query = "UPDATE activities SET activity_time = ? WHERE user_id = (SELECT id FROM Users WHERE tg_id = ?) AND category = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setTime(1, newTime);
            statement.setInt(2, tgId);
            statement.setString(3, category);
            statement.executeUpdate();

            logger.log(Level.INFO, "Activity time updated successfully: tgId={0}, category={1}, newTime={2}", new Object[]{tgId, category, newTime});

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating activity time: tgId=" + tgId + ", category=" + category + ", newTime=" + newTime, e);
        }
    }

    public List<Reminder> getAllReminders() {
        String query = "SELECT u.tg_id, a.category, a.activity_time, a.streak_num FROM activities a JOIN Users u ON a.user_id = u.id";
        List<Reminder> reminders = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int tgId = resultSet.getInt("tg_id");
                String category = resultSet.getString("category");
                Time activityTime = resultSet.getTime("activity_time");
                int streakNum = resultSet.getInt("streak_num");

                reminders.add(new Reminder(tgId, category, activityTime, streakNum));
            }

            logger.log(Level.INFO, "Retrieved {0} reminders from the database", reminders.size());

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving reminders", e);
        }

        return reminders;
    }


    public List<Reminder> getAllRemindersForUser(int tgId) {
        String query = "SELECT a.category, a.activity_time, a.streak_num FROM activities a JOIN users u ON a.user_id = u.id WHERE u.tg_id = ?";
        List<Reminder> reminders = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, tgId);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String category = resultSet.getString("category");
                Time activityTime = resultSet.getTime("activity_time");
                int streakNum = resultSet.getInt("streak_num");

                reminders.add(new Reminder(tgId, category, activityTime, streakNum));
            }

            logger.log(Level.INFO, "Retrieved {0} reminders for user {1} from the database", new Object[]{reminders.size(), tgId});

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving reminders for user " + tgId, e);
        }

        return reminders;
    }


    public void deleteActivity(int tgId, String category) {
        String query = "DELETE FROM activities WHERE user_id = (SELECT id FROM Users WHERE tg_id = ?) AND category = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, tgId);
            statement.setString(2, category);
            statement.executeUpdate();

            logger.log(Level.INFO, "Activity deleted successfully: tgId={0}, category={1}", new Object[]{tgId, category});

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting activity: tgId=" + tgId + ", category=" + category, e);
        }
    }

    public void incrementStreakNum(int tgId, String category) {
        String query = "UPDATE activities SET streak_num = streak_num + 1 WHERE user_id = (SELECT id FROM Users WHERE tg_id = ?) AND category = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, tgId);
            statement.setString(2, category);
            statement.executeUpdate();

            logger.log(Level.INFO, "Streak number incremented for userId={0}, category={1}", new Object[]{tgId, category});

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error incrementing streak number for userId=" + tgId + ", category=" + category, e);
        }
    }

    public void resetStreakNum(int tgId, String category) {
        String query = "UPDATE activities SET streak_num = 0 WHERE user_id = (SELECT id FROM Users WHERE tg_id = ?) AND category = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, tgId);
            statement.setString(2, category);
            statement.executeUpdate();

            logger.log(Level.INFO, "Streak number reset for userId={0}, category={1}", new Object[]{tgId, category});

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error resetting streak number for userId=" + tgId + ", category=" + category, e);
        }
    }

    public int getStreakNum(int tgId, String category) {
        String query = "SELECT streak_num FROM activities a JOIN Users u ON a.user_id = u.id WHERE u.tg_id = ? AND a.category = ?";
        int streakNum = 0;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, tgId);
            statement.setString(2, category);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                streakNum = resultSet.getInt("streak_num");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving streak number for userId=" + tgId + ", category=" + category, e);
        }

        return streakNum;
    }

    public static class Reminder {
        private final int userId;
        private final String category;
        private final Time activityTime;
        private final int streakNum;

        public Reminder(int userId, String category, Time activityTime, int streakNum) {
            this.userId = userId;
            this.category = category;
            this.activityTime = activityTime;
            this.streakNum = streakNum;
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

//        public int getStreakNum() {
//            return streakNum;
//        }
    }

}
