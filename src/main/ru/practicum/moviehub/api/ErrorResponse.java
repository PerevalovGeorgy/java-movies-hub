package ru.practicum.moviehub.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.http.BaseHttpHandler;

import java.io.IOException;
import java.util.List;

public record ErrorResponse(String error, List<String> details) {
    private static final Gson gson = new Gson();

    public String toJson() {
        return gson.toJson(this);
    }

    public static void sendValidationError(HttpExchange exchange, BaseHttpHandler handler, List<String> details) throws IOException {
        ErrorResponse error = new ErrorResponse("Ошибка валидации", details);
        handler.sendJson(exchange, 422, error.toJson());
    }

    public static void sendUnsupportedMediaType(HttpExchange exchange, BaseHttpHandler handler) throws IOException {
        String errorJson = "{\"error\": \"Неподдерживаемый тип контента. Ожидается application/json\"}";
        handler.sendJson(exchange, 415, errorJson);
    }

    public static void sendBadRequest(HttpExchange exchange, BaseHttpHandler handler, String message) throws IOException {
        String errorJson = String.format("{\"error\": \"%s\"}", message);
        handler.sendJson(exchange, 400, errorJson);
    }

    public static void sendNotFound(HttpExchange exchange, BaseHttpHandler handler, String message) throws IOException {
        String errorJson = String.format("{\"error\": \"%s\"}", message);
        handler.sendJson(exchange, 404, errorJson);
    }

    public static void sendMethodNotAllowed(HttpExchange exchange, BaseHttpHandler handler) throws IOException {
        String errorJson = "{\"error\": \"Метод не поддерживается\"}";
        exchange.getResponseHeaders().set("Allow", "GET, POST, DELETE");
        handler.sendJson(exchange, 405, errorJson);
    }

    public static void sendInternalError(HttpExchange exchange, BaseHttpHandler handler, String message) throws IOException {
        String errorJson = String.format("{\"error\": \"Внутренняя ошибка сервера: %s\"}", message);
        handler.sendJson(exchange, 500, errorJson);
    }
}