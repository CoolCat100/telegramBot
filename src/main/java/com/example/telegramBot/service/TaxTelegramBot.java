package com.example.telegramBot.service;

import com.example.telegramBot.config.BotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TaxTelegramBot extends TelegramLongPollingBot {
    @Autowired
    private TaxService taxService;
    @Autowired
    private BotConfig botConfig;

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotKey();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            if (update.getMessage().isGroupMessage() || update.getMessage().isSuperGroupMessage()) {
                if (messageText.contains("@" + botConfig.getBotName())) {
                    handleMention(update, messageText);
                }
            } else {
                handleDirectMessage(update, messageText);
            }
        }
    }

    private void handleMention(Update update, String messageText) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(update.getMessage().getChatId()));
        message.enableMarkdown(true);

        String[] parts = messageText.split("\\s+");
        boolean numberFound = false;
        for (String part : parts) {
            double salary = parseSalary(part);
            if (salary != -1) {
                processSalary(update, salary);
                numberFound = true;
                break;
            }
        }
        if (!numberFound) {
            message.setText("Пожалуйста, укажите сумму зарплаты в числовом формате после упоминания бота.");
            sendResponse(update, message);
        }
    }

    private void handleDirectMessage(Update update, String messageText) {
        double salary = parseSalary(messageText);
        if (salary != -1) {
            processSalary(update, salary);
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(update.getMessage().getChatId()));
            message.enableMarkdown(true);
            message.setText("Пожалуйста, отправьте зарплату в числовом формате");
            sendResponse(update, message);
        }
    }

    private void processSalary(Update update, double salary) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(update.getMessage().getChatId()));
        message.enableMarkdown(true);

        double tax = taxService.countOldTax(salary);
        double finalSalary = salary - tax;
        message.setText("*Размер зарплаты net:* " + finalSalary);
        sendResponse(update, message);
    }

    private void sendResponse(Update update, SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            Logger logger = LoggerFactory.getLogger(TaxTelegramBot.class);
            logger.error("Error sending message", e);
        }
    }

    private double parseSalary(String input) {
        if (input.toLowerCase().endsWith("к")) {
            input = input.substring(0, input.length() - 1); // Удаляем последний символ 'к'
            try {
                return Double.parseDouble(input) * 1000; // Умножаем на 1000
            } catch (NumberFormatException e) {
                return -1; // Возвращаем -1, если ввод не является числом
            }
        } else {
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }
}
