package org.example.repository;

import org.example.database.DatabaseConnection;
import org.example.reminder.Reminder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс репозитория для работы с пользователями и их действиями в базе данных.
 */
public class UserRepository {
    private static final Logger logger = Logger.getLogger(UserRepository.class.getName());

    /**
     * Добавляет нового пользователя в базу данных.
     *
     * @param tgId      Телеграм ID пользователя.
     * @param userName  Имя пользователя.
     */
    public void addUser(int tgId, String userName) {
        String query = "INSERT INTO Users (tg_id, user_name) VALUES (?, ?) ON CONFLICT (tg_id) DO NOTHING";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, tgId);
            statement.setString(2, userName);
            int rowsAffected = statement.executeUpdate();
            logUserAdditionResult(tgId, userName, rowsAffected);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при добавлении пользователя: tgId=" + tgId + ", userName=" + userName, e);
        }
    }

    private void logUserAdditionResult(int tgId, String userName, int rowsAffected) {
        if (rowsAffected > 0) {
            logger.log(Level.INFO, "Пользователь успешно добавлен: tgId={0}, userName={1}", new Object[]{tgId, userName});
        } else {
            logger.log(Level.INFO, "Пользователь уже существует: tgId={0}", tgId);
        }
    }

    /**
     * Добавляет новое действие для пользователя.
     *
     * @param tgId         Телеграм ID пользователя.
     * @param category     Категория действия.
     * @param activityTime Время действия.
     */
    public void addActivity(int tgId, String category, Time activityTime) {
        String query = "INSERT INTO activities (user_id, category, activity_time) VALUES ((SELECT id FROM Users WHERE tg_id = ?), ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, tgId);
            statement.setString(2, category);
            statement.setTime(3, activityTime);
            statement.executeUpdate();
            logger.log(Level.INFO, "Действие успешно добавлено: tgId={0}, category={1}, activityTime={2}", new Object[]{tgId, category, activityTime});
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при добавлении действия: tgId=" + tgId + ", category=" + category + ", activityTime=" + activityTime, e);
        }
    }

    /**
     * Возвращает имя пользователя по Телеграм ID.
     *
     * @param tgId Телеграм ID пользователя.
     * @return Optional с именем пользователя, если найден, иначе пустой Optional.
     */
    public Optional<String> getUserNameByTgId(int tgId) {
        String query = "SELECT user_name FROM Users WHERE tg_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, tgId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String userName = resultSet.getString("user_name");
                logger.log(Level.INFO, "Пользователь найден: tgId={0}, userName={1}", new Object[]{tgId, userName});
                return Optional.of(userName);
            } else {
                logger.log(Level.INFO, "Пользователь не найден: tgId={0}", tgId);
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при получении пользователя: tgId=" + tgId, e);
            return Optional.empty();
        }
    }

    /**
     * Возвращает время действия по Телеграм ID и категории.
     *
     * @param tgId     Телеграм ID пользователя.
     * @param category Категория действия.
     * @return Optional с временем действия, если найдено, иначе пустой Optional.
     */
    public Optional<Time> getActivityTime(int tgId, String category) {
        String query = "SELECT activity_time FROM activities a JOIN Users u ON a.user_id = u.id WHERE u.tg_id = ? AND a.category = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, tgId);
            statement.setString(2, category);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Time activityTime = resultSet.getTime("activity_time");
                return Optional.of(activityTime);
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при получении времени действия для пользователя: tgId=" + tgId + ", category=" + category, e);
            return Optional.empty();
        }
    }

    /**
     * Обновляет время действия для пользователя.
     *
     * @param tgId      Телеграм ID пользователя.
     * @param category  Категория действия.
     * @param newTime   Новое время действия.
     */
    public void updateActivityTime(int tgId, String category, Time newTime) {
        String query = "UPDATE activities SET activity_time = ? WHERE user_id = (SELECT id FROM Users WHERE tg_id = ?) AND category = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setTime(1, newTime);
            statement.setInt(2, tgId);
            statement.setString(3, category);
            statement.executeUpdate();
            logger.log(Level.INFO, "Время действия успешно обновлено: tgId={0}, category={1}, newTime={2}", new Object[]{tgId, category, newTime});
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при обновлении времени действия: tgId=" + tgId + ", category=" + category + ", newTime=" + newTime, e);
        }
    }

    /**
     * Возвращает список всех напоминаний из базы данных.
     *
     * @return Список напоминаний.
     */
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
                reminders.add(new Reminder(tgId, category, activityTime));
            }
            logger.log(Level.INFO, "Получено {0} напоминаний из базы данных", reminders.size());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при получении напоминаний", e);
        }
        return reminders;
    }

    /**
     * Возвращает список всех напоминаний для конкретного пользователя.
     *
     * @param tgId Телеграм ID пользователя.
     * @return Список напоминаний для пользователя.
     */
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
                reminders.add(new Reminder(tgId, category, activityTime));
            }
            logger.log(Level.INFO, "Получено {0} напоминаний для пользователя {1} из базы данных", new Object[]{reminders.size(), tgId});
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при получении напоминаний для пользователя " + tgId, e);
        }
        return reminders;
    }

    /**
     * Удаляет действие для пользователя.
     *
     * @param tgId     Телеграм ID пользователя.
     * @param category Категория действия.
     */
    public void deleteActivity(int tgId, String category) {
        String query = "DELETE FROM activities WHERE user_id = (SELECT id FROM Users WHERE tg_id = ?) AND category = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, tgId);
            statement.setString(2, category);
            statement.executeUpdate();
            logger.log(Level.INFO, "Действие успешно удалено: tgId={0}, category={1}", new Object[]{tgId, category});
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при удалении действия: tgId=" + tgId + ", category=" + category, e);
        }
    }

    /**
     * Увеличивает количество выполнений для действия пользователя.
     *
     * @param tgId     Телеграм ID пользователя.
     * @param category Категория действия.
     */
    public void incrementStreakNum(int tgId, String category) {
        String query = "UPDATE activities SET streak_num = streak_num + 1 WHERE user_id = (SELECT id FROM Users WHERE tg_id = ?) AND category = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, tgId);
            statement.setString(2, category);
            statement.executeUpdate();
            logger.log(Level.INFO, "Количество выполнений увеличено для пользователя: tgId={0}, category={1}", new Object[]{tgId, category});
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при увеличении количества выполнений для пользователя: tgId=" + tgId + ", category=" + category, e);
        }
    }

    /**
     * Сбрасывает количество выполнений для действия пользователя.
     *
     * @param tgId     Телеграм ID пользователя.
     * @param category Категория действия.
     */
    public void resetStreakNum(int tgId, String category) {
        String query = "UPDATE activities SET streak_num = 0 WHERE user_id = (SELECT id FROM Users WHERE tg_id = ?) AND category = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, tgId);
            statement.setString(2, category);
            statement.executeUpdate();
            logger.log(Level.INFO, "Количество выполнений сброшено для пользователя: tgId={0}, category={1}", new Object[]{tgId, category});
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при сбросе количества выполнений для пользователя: tgId=" + tgId + ", category=" + category, e);
        }
    }

    /**
     * Возвращает количество выполнений для действия пользователя.
     *
     * @param tgId     Телеграм ID пользователя.
     * @param category Категория действия.
     * @return Количество выполнений.
     */
    public int getStreakNum(int tgId, String category) {
        String query = "SELECT streak_num FROM activities a JOIN Users u ON a.user_id = u.id WHERE u.tg_id = ? AND a.category = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, tgId);
            statement.setString(2, category);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("streak_num");
            } else {
                return 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при получении количества выполнений для пользователя: tgId=" + tgId + ", category=" + category, e);
            return 0;
        }
    }
}
