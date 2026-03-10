package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.store.MoviesStore;
import ru.practicum.moviehub.store.Movie;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private final Gson gson = new Gson();
    private static final int MIN_YEAR = 1888;
    public static final int MAX_YEAR = Year.now().getValue() + 1;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        try {
            switch (method) {
                case "GET":
                    handleGetRequest(exchange, path, query);
                    break;
                case "POST":
                    handlePostRequest(exchange);
                    break;
                case "DELETE":
                    handleDeleteRequest(exchange, path);
                    break;
                default:
                    ErrorResponse.sendMethodNotAllowed(exchange, this);
            }
        } catch (Exception e) {
            ErrorResponse.sendInternalError(exchange,this, e.getMessage());
        }
    }

    private void handleGetRequest(HttpExchange exchange, String path, String query) throws IOException {
        if (query != null && query.startsWith("year=")) {
            handleFilterByYear(exchange, query);
            return;
        }

        if (path.startsWith("/movies/")) {
            String idPart = path.substring("/movies/".length());

            if (idPart.isEmpty()) {
                ErrorResponse.sendNotFound(exchange, this,"Путь не найден");
                return;
            }

            if (!idPart.matches("\\d+")) {
                ErrorResponse.sendBadRequest(exchange, this,"Некорректный ID");
                return;
            }

            handleGetById(exchange, path);
            return;
        }

        if (path.equals("/movies")) {
            List<Movie> movies = moviesStore.getAllMovies();
            sendJson(exchange, 200, gson.toJson(movies));
            return;
        }

        ErrorResponse.sendNotFound(exchange, this,"Путь не найден: " + path);
    }

    private void handleGetById(HttpExchange exchange, String path) throws IOException {
        try {
            int id = Integer.parseInt(path.substring("/movies/".length()));
            Optional<Movie> movie = moviesStore.getMovieById(id);

            if (movie.isPresent()) {
                sendJson(exchange, 200, gson.toJson(movie.get()));
            } else {
                ErrorResponse.sendNotFound(exchange, this,"Фильм не найден");
            }
        } catch (NumberFormatException e) {
            ErrorResponse.sendBadRequest(exchange, this,"Некорректный ID");
        }
    }

    private void handleFilterByYear(HttpExchange exchange, String query) throws IOException {
        String yearParam = query.substring(5); // убираем "year="

        try {
            int year = Integer.parseInt(yearParam);

            if (year < MIN_YEAR || year > MAX_YEAR) {
                ErrorResponse.sendBadRequest(exchange, this,"Некорректный параметр запроса — 'year'");
                return;
            }

            List<Movie> movies = moviesStore.getMoviesByYear(year);
            sendJson(exchange, 200, gson.toJson(movies));

        } catch (NumberFormatException e) {
            ErrorResponse.sendBadRequest(exchange, this,"Некорректный параметр запроса — 'year'");
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("application/json")) {
            ErrorResponse.sendUnsupportedMediaType(exchange, this);
            return;
        }

        if (!exchange.getRequestURI().getPath().equals("/movies")) {
            ErrorResponse.sendNotFound(exchange, this, "Путь не найден");
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            Movie movie = gson.fromJson(reader, Movie.class);
            List<String> validationErrors = validateMovie(movie);

            if (!validationErrors.isEmpty()) {
                ErrorResponse.sendValidationError(exchange,this, validationErrors);
                return;
            }

            Movie created = moviesStore.addMovie(movie);
            sendJson(exchange, 201, gson.toJson(created));

        } catch (JsonSyntaxException e) {
            ErrorResponse.sendBadRequest(exchange, this,"Некорректный JSON");
        }
    }

    private void handleDeleteRequest(HttpExchange exchange, String path) throws IOException {
        if (!path.startsWith("/movies/")) {
            ErrorResponse.sendNotFound(exchange,this, "Путь не найден");
            return;
        }

        String idPart = path.substring("/movies/".length());

        if (idPart.isEmpty()) {
            ErrorResponse.sendNotFound(exchange,this, "Путь не найден");
            return;
        }

        if (!idPart.matches("\\d+")) {
            ErrorResponse.sendBadRequest(exchange,this, "Некорректный ID");
            return;
        }

        try {
            int id = Integer.parseInt(idPart);
            if (moviesStore.deleteMovie(id)) {
                sendNoContent(exchange);
            } else {
                ErrorResponse.sendNotFound(exchange, this,"Фильм не найден");
            }
        } catch (NumberFormatException e) {
            ErrorResponse.sendBadRequest(exchange,this, "Некорректный ID");
        }
    }

    private List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();
        int currentYear = Year.now().getValue();

        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            errors.add("название должно быть не длиннее 100 символов");
        }

        if (movie.getYear() == null) {
            errors.add("год должен быть указан");
        } else if (movie.getYear() < MIN_YEAR || movie.getYear() > currentYear + 1) {
            errors.add(String.format("год должен быть между %d и %d", MIN_YEAR, currentYear + 1));
        }

        return errors;
    }

}