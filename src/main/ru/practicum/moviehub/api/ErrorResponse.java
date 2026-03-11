package ru.practicum.moviehub.api;

import com.google.gson.Gson;
import java.util.List;

public record ErrorResponse(String error, List<String> details) {
    private static final Gson gson = new Gson();

    public static final String VALIDATION_ERROR = "Ошибка валидации";

    public static String unsupportedMediaType() {
        return "{\"error\": \"Неподдерживаемый тип контента. Ожидается application/json\"}";
    }

    public static String badRequest(String message) {
        return String.format("{\"error\": \"%s\"}", message);
    }

    public static String notFound(String message) {
        return String.format("{\"error\": \"%s\"}", message);
    }

    public static String methodNotAllowed() {
        return "{\"error\": \"Метод не поддерживается\"}";
    }

    public static String internalError(String message) {
        return String.format("{\"error\": \"Внутренняя ошибка сервера: %s\"}", message);
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static ErrorResponse fromJson(String json) {
        return gson.fromJson(json, ErrorResponse.class);
    }

    public static ErrorResponse validationError(List<String> details) {
        return new ErrorResponse(VALIDATION_ERROR, details);
    }
}