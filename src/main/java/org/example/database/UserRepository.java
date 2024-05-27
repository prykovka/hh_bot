package org.example.database;

import java.sql.*;
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
}