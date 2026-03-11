package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.store.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import ru.practicum.moviehub.api.ErrorResponse;
import static org.junit.jupiter.api.Assertions.*;
import static ru.practicum.moviehub.http.GetMoviesApiTest.assertContentType;
import static ru.practicum.moviehub.http.MoviesHandler.MAX_YEAR;

public class PostMoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore;
    private static final Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        moviesStore = new MoviesStore();
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
        int randomYear = ThreadLocalRandom.current().nextInt(1888, MAX_YEAR + 1);
        Movie newMovie = new Movie("Inception", randomYear);
        String jsonBody = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());
        assertContentType(resp);

        Movie createdMovie = gson.fromJson(resp.body(), Movie.class);

        assertNotNull(createdMovie.getId());
        assertTrue(createdMovie.getId() > 0);
        assertEquals("Inception", createdMovie.getTitle());
        assertEquals(randomYear, createdMovie.getYear());

    }

    @Test
    void postMovies_whenEmptyTitle_returns422() throws Exception {
        Movie invalidMovie = new Movie("", 2020);
        String jsonBody = gson.toJson(invalidMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertContentType(resp);

        JsonObject jsonObject = gson.fromJson(resp.body(), JsonObject.class);

        String errorMsg = jsonObject.get("error").getAsString();
        JsonArray detailsArray = jsonObject.getAsJsonArray("details");
        List<String> details = gson.fromJson(detailsArray, new TypeToken<List<String>>(){}.getType());

        ErrorResponse error = new ErrorResponse(errorMsg, details);

        assertEquals("Ошибка валидации", error.error());
        assertTrue(error.details().contains("название не должно быть пустым"));
    }

    @Test
    void postMovies_whenTitleTooLong_returns422() throws Exception {
        String longTitle = "a".repeat(101);
        Movie invalidMovie = new Movie(longTitle, 2020);
        String jsonBody = gson.toJson(invalidMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Ошибка валидации", error.get("error").getAsString());

        String details = error.get("details").getAsJsonArray().toString();
        assertTrue(details.contains("название должно быть не длиннее 100 символов"));
    }

    @Test
    void postMovies_whenYearTooLow_returns422() throws Exception {
        Movie invalidMovie = new Movie("Inception", 1800);
        String jsonBody = gson.toJson(invalidMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Ошибка валидации", error.get("error").getAsString());

        String details = error.get("details").getAsJsonArray().toString();
        assertTrue(details.contains("год должен быть между 1888 и " + MAX_YEAR));
    }

    @Test
    void postMovies_whenYearTooHigh_returns422() throws Exception {
        Movie invalidMovie = new Movie("Inception", MAX_YEAR + 1);
        String jsonBody = gson.toJson(invalidMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Ошибка валидации", error.get("error").getAsString());

        String details = error.get("details").getAsJsonArray().toString();
        assertTrue(details.contains("год должен быть между 1888 и " + MAX_YEAR));
    }

    @Test
    void postMovies_whenInvalidContentType_returns415() throws Exception {
        Movie movie = new Movie("Inception", 2020);
        String jsonBody = gson.toJson(movie);

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
        String invalidJson = "{\"title\": \"Inception\" \"year\": 2020}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertTrue(error.has("error"));
    }


}