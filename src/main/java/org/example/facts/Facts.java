package org.example.facts;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Facts {
    private static final Map<String, String[]> facts = new HashMap<>();
    private static final Random random = new Random();

    static {
        facts.put("water", new String[]{
                "Питьевая вода помогает поддерживать баланс жидкости в организме.",
                "Вода улучшает работу мозга и концентрацию.",
                "Питьевая вода помогает поддерживать энергию и снижать усталость."
        });
        facts.put("exercise", new String[]{
                "Физическая активность укрепляет сердечно-сосудистую систему.",
                "Регулярные тренировки улучшают настроение и снижают стресс.",
                "Упражнения помогают поддерживать здоровый вес."
        });
        facts.put("sleep", new String[]{
                "Качественный сон улучшает память и концентрацию.",
                "Сон помогает организму восстанавливаться и поддерживать иммунную систему.",
                "Достаточное количество сна снижает риск хронических заболеваний."
        });
        facts.put("read", new String[]{
                "Чтение развивает мышление и улучшает концентрацию.",
                "Регулярное чтение улучшает словарный запас и навыки письма.",
                "Чтение помогает снизить уровень стресса и улучшить сон."
        });
    }

    public static String getRandomFact(String category) {
        if (!facts.containsKey(category)) {
            return "Нет фактов для этой категории.";
        }
        String[] categoryFacts = facts.get(category);
        int index = random.nextInt(categoryFacts.length);
        return categoryFacts[index];
    }
}
