package org.example.database;

import org.example.config.ConfigLoader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Утилита для управления подключениями к базе данных.
 */
public class DatabaseConnection {
    private static final String URL = ConfigLoader.getProperty("db.url");
    private static final String USER = ConfigLoader.getProperty("db.user");
    private static final String PASSWORD = ConfigLoader.getProperty("db.password");

    private DatabaseConnection() {
    }

    /**
     * Получает соединение с базой данных.
     *
     * @return Соединение с базой данных.
     * @throws SQLException Если происходит ошибка доступа к базе данных.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
