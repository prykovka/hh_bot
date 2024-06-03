package org.example.menu;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;

public class Menu {
    public static InlineKeyboardMarkup getCategoryMenu() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("Вода💧").callbackData("water"),
                        new InlineKeyboardButton("Спорт💪").callbackData("exercise")
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("Сон💤").callbackData("sleep"),
                        new InlineKeyboardButton("Чтение📚").callbackData("read")
                }
        );
    }

    public static InlineKeyboardMarkup getFactsMenu() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("Вода\uD83D\uDCA6").callbackData("fact_water"),
                        new InlineKeyboardButton("Спорт\uD83C\uDFC6 ").callbackData("fact_exercise")
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("Сон\uD83D\uDE34").callbackData("fact_sleep"),
                        new InlineKeyboardButton("Чтение\uD83D\uDCD6").callbackData("fact_read")
                }
        );
    }
}
