package org.example.database;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomReminderRepository {
    private static final Logger logger = Logger.getLogger(CustomReminderRepository.class.getName());

    public void addCustomReminder(int tgId, String customText) {
        String query = "INSERT INTO customs (user_id, custom_text) VALUES ((SELECT id FROM Users WHERE tg_id = ?), ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, tgId);
            statement.setString(2, customText);
            statement.executeUpdate();

            logger.log(Level.INFO, "Custom reminder added successfully: tgId={0}, customText={1}", new Object[]{tgId, customText});

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding custom reminder: tgId=" + tgId + ", customText=" + customText, e);
        }
    }

    public String getCustomReminderText(int tgId) {
        String query = "SELECT custom_text FROM customs WHERE user_id = (SELECT id FROM Users WHERE tg_id = ?)";
        String reminderText = null;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, tgId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                reminderText = resultSet.getString("custom_text");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving custom reminder text: tgId=" + tgId, e);
        }

        return reminderText;
    }
}
