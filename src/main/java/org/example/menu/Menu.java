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
                        new InlineKeyboardButton("Вода💧").callbackData("fact_water"),
                        new InlineKeyboardButton("Спорт💪").callbackData("fact_exercise")
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("Сон💤").callbackData("fact_sleep"),
                        new InlineKeyboardButton("Чтение📚").callbackData("fact_read")
                }
        );
    }
}
