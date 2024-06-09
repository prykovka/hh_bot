package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс для загрузки конфигурационных свойств из файла application.properties.
 */
public class ConfigLoader {
    private static final Logger logger = Logger.getLogger(ConfigLoader.class.getName());
    private static final Properties properties = new Properties();

    private ConfigLoader(){
    }

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.log(Level.SEVERE, "Не найдено application.properties");
            } else {
                properties.load(input);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Ошибка загрузки application.properties", ex);
        }
    }

    /**
     * Получает значение свойства по ключу.
     *
     * @param key Ключ свойства.
     * @return Значение свойства.
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Получает значение свойства по ключу и преобразует его в целое число.
     *
     * @param key Ключ свойства.
     * @return Значение свойства как целое число.
     */
    public static int getIntProperty(String key) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE, "Невозможно обработать: " + key + ". Причина: " + e);
            }
        }
        return 0;
    }
}
