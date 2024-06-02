package org.example.menu;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;

public class Menu {
    public static InlineKeyboardMarkup getCategoryMenu() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("Вода\uD83D\uDCA6").callbackData("water"),
                        new InlineKeyboardButton("Спорт\uD83D\uDCAA").callbackData("exercise")
                },
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("Сон\uD83D\uDCA4").callbackData("sleep"),
                        new InlineKeyboardButton("Чтение\uD83D\uDCDA").callbackData("read")
                }
        );
    }
}