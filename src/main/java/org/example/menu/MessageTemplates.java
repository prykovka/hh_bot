package org.example.menu;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class MessageTemplates {
    private static final Map<String, List<String>> TEMPLATES = Map.of(
            "water", List.of(
                    "%s, выпей стакан воды и твоя мама будет жить вечно.",
                    "%s, не забудь выпить воды для здоровья.",
                    "%s, пора пить воду! Гидратация — это важно."
            ),
            "exercise", List.of(
                    "%s, время сделать зарядку!",
                    "%s, пора размяться и сделать несколько упражнений.",
                    "%s, не забудь про свою тренировку сегодня!"
            ),
            "sleep", List.of(
                    "%s, пора готовиться ко сну.",
                    "%s, не забудь ложиться спать вовремя.",
                    "%s, время для сна! Отдых важен."
            ),
            /*
            "education", List.of(
                    "%s, настало время для учебы.",
                    "%s, пора заняться чем-то новым и полезным.",
                    "%s, не забудь учиться каждый день!"
            ),
            "walk", List.of(
                    "%s, пора отправиться на прогулку.",
                    "%s, время выйти на свежий воздух.",
                    "%s, не забудь про прогулку сегодня!"
            ),
             */
            "read", List.of(
                    "%s, настало время для твоей любимой книги.",
                    "%s, пора почитать что-нибудь интересное.",
                    "%s, время для чтения! Найди минутку для книги."
            ),
            "custom", List.of(
                    "%s, ты просил напомнить о"
            )
    );
    private static final Random RANDOM = new Random();

    public static String getRandomMessage(String category, String userName) {
        List<String> messages = TEMPLATES.get(category);
        if (messages != null && !messages.isEmpty()) {
            String template = messages.get(RANDOM.nextInt(messages.size()));
            return String.format(template, userName);
        }
        return userName + ", у вас есть напоминание.";
    }
}
