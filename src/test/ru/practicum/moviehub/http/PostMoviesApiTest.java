package ru.practicum.moviehub.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class PostMoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore;

    @BeforeAll
    static void beforeAll() {
        MoviesStore moviesStore = new MoviesStore();
        server = new MoviesServer(moviesStore, 8080);
        client =  HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();


        server.start();
    }

    @BeforeEach
    void beforeEach() {
        if (moviesStore != null) {
            moviesStore.clear();
        }
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void postMovies_addFilmSuccess() throws Exception {
        int randomYear = ThreadLocalRandom.current().nextInt(1888, MoviesHandler.MAX_YEAR + 1);
        String jsonBody = String.format("{title: Inception, year: %d}", randomYear);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());
        GetMoviesApiTest.assertContentType(resp);

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertTrue(jsonObject.has("id"));
        assertEquals("Inception", jsonObject.get("title").getAsString());
        assertEquals(randomYear, jsonObject.get("year").getAsInt());

    }

    @Test
    void postMovies_whenEmptyTitle_returns422() throws Exception {
        String jsonBody = "{\"title\": \"\", \"year\": 2020}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        GetMoviesApiTest.assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Ошибка валидации", error.get("error").getAsString());

        JsonArray details = error.get("details").getAsJsonArray();
        assertTrue(details.toString().contains("название не должно быть пустым"));
    }

    @Test
    void postMovies_whenTitleTooLong_returns422() throws Exception {
        String longTitle = "a".repeat(101);
        String jsonBody = String.format("{\"title\": \"%s\", \"year\": 2020}", longTitle);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        GetMoviesApiTest.assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Ошибка валидации", error.get("error").getAsString());

        JsonArray details = error.get("details").getAsJsonArray();
        assertTrue(details.toString().contains("название должно быть не длиннее 100 символов"));
    }

    @Test
    void postMovies_whenYearTooLow_returns422() throws Exception {
        String jsonBody = "{\"title\": \"Inception\", \"year\": 1800}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        GetMoviesApiTest.assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Ошибка валидации", error.get("error").getAsString());

        String details = error.get("details").getAsJsonArray().toString();
        assertTrue(details.contains("год должен быть между 1888 и " + MoviesHandler.MAX_YEAR));
    }

    @Test
    void postMovies_whenYearTooHigh_returns422() throws Exception {
        String jsonBody = String.format("{\"title\": \"Inception\", \"year\": %d}", MoviesHandler.MAX_YEAR + 1);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        GetMoviesApiTest.assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Ошибка валидации", error.get("error").getAsString());

        String details = error.get("details").getAsJsonArray().toString();
        assertTrue(details.contains("год должен быть между 1888 и " + MoviesHandler.MAX_YEAR));
    }

    @Test
    void postMovies_whenInvalidContentType_returns415() throws Exception {
        String jsonBody = "{\"title\": \"Inception\", \"year\": 2020}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode());
        assertTrue(resp.headers().firstValue("Content-Type").isPresent());
    }

    @Test
    void postMovies_whenInvalidJson_returns400() throws Exception {
        String invalidJson = "{title Inception, year 2020}"; // невалидный JSON

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        GetMoviesApiTest.assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertTrue(error.has("error"));
    }


}