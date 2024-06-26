package org.example.templates.messages;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Класс MessagesTemplates предоставляет шаблоны сообщений для различных категорий напоминаний.
 */
public class MessagesTemplates {

    private MessagesTemplates(){
    }

    // Шаблоны сообщений для различных категорий
    private static final Map<String, List<String>> TEMPLATES = Map.of(
            "water", List.of(
                    "%s, выпей стакан воды и твоя мама будет жить вечно\uD83D\uDCA6",
                    "%s, не забудь выпить воды для здоровья\uD83D\uDCA6",
                    "%s, пора пить воду! Гидратация — это важно\uD83D\uDCA6"
            ),
            "exercise", List.of(
                    "%s, время сделать зарядку!\uD83D\uDCAA",
                    "%s, пора размяться и сделать несколько упражнений!\uD83D\uDCAA",
                    "%s, не забудь про свою тренировку сегодня!\uD83D\uDCAA"
            ),
            "sleep", List.of(
                    "%s, пора готовиться ко сну\uD83D\uDCA4",
                    "%s, не забудь лечь спать сегодня вовремя\uD83D\uDCA4",
                    "%s, время для сна! Отдых важен\uD83D\uDCA4"
            ),
            "read", List.of(
                    "%s, настало время для твоей любимой книги\uD83D\uDCDA",
                    "%s, пора почитать что-нибудь интересное\uD83D\uDCDA",
                    "%s, время для чтения! Найди минутку для книги\uD83D\uDCDA"
            )
    );

    // Генератор случайных чисел
    private static final Random RANDOM = new Random();

    /**
     * Возвращает случайное сообщение для заданной категории и имени пользователя.
     *
     * @param category Категория сообщения (например, "water", "exercise", "sleep", "read").
     * @param userName Имя пользователя, которое будет вставлено в сообщение.
     * @return Случайное сообщение для указанной категории, содержащее имя пользователя.
     */
    public static String getRandomMessage(String category, String userName) {
        List<String> messages = TEMPLATES.get(category);
        if (messages != null && !messages.isEmpty()) {
            String template = messages.get(RANDOM.nextInt(messages.size()));
            return String.format(template, userName);
        }
        return userName + ", у вас есть напоминание.";
    }
}
