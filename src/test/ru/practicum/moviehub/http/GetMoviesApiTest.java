package ru.practicum.moviehub.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.*;

public class GetMoviesApiTest {
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
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE+"/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenNoEmpty_returnsArray() throws Exception {

        server.addMovie(new Movie("Начало", 2010));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE+"/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonElement jsonElement = JsonParser.parseString(resp.body());
        assertTrue(jsonElement.isJsonArray(), "Ожидается JSON массив");
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        assertEquals(1, jsonArray.size(), "Должен быть 1 фильм");
        JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
        String title = jsonObject.get("title").getAsString();
        assertEquals("Начало", title);
        int year = jsonObject.get("year").getAsInt();
        assertEquals(2010, year);

    }

    @Test
    void getMovies_whenHasMovies_returnsMoviesArray() throws Exception {
        server.addMovie(new Movie("Начало", 2010));
        server.addMovie(new Movie("Матрица", 1999));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        JsonArray jsonArray = JsonParser.parseString(resp.body()).getAsJsonArray();
        assertEquals(2, jsonArray.size());

        JsonObject firstMovie = jsonArray.get(0).getAsJsonObject();
        assertTrue(firstMovie.has("id"));
        assertEquals("Начало", firstMovie.get("title").getAsString());
        assertEquals(2010, firstMovie.get("year").getAsInt());
    }


    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie movie = server.addMovie(new Movie("Начало", 2010));
        int id = movie.getId();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals(id, jsonObject.get("id").getAsInt());
        assertEquals("Начало", jsonObject.get("title").getAsString());
        assertEquals(2010, jsonObject.get("year").getAsInt());
    }

    @Test
    void getMovieById_whenNotExists_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Фильм не найден", error.get("error").getAsString());
    }

    @Test
    void getMovieById_whenIdNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Некорректный ID", error.get("error").getAsString());
    }

    @Test
    void getMoviesByYear_whenHasMovies_returnsFilteredMovies() throws Exception {
        server.addMovie(new Movie("Начало", 2010));
        server.addMovie(new Movie("Матрица", 1999));
        server.addMovie(new Movie("Интерстеллар", 2010));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2010"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        JsonArray jsonArray = JsonParser.parseString(resp.body()).getAsJsonArray();
        assertEquals(2, jsonArray.size());

        // Проверяем, что все фильмы 2010 года
        for (JsonElement element : jsonArray) {
            assertEquals(2010, element.getAsJsonObject().get("year").getAsInt());
        }
    }

    @Test
    void getMoviesByYear_whenNoMovies_returnsEmptyArray() throws Exception {
        server.addMovie(new Movie("Начало", 2010));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2020"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        JsonArray jsonArray = JsonParser.parseString(resp.body()).getAsJsonArray();
        assertEquals(0, jsonArray.size());
    }

    @Test
    void getMoviesByYear_whenYearNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Некорректный параметр запроса — 'year'", error.get("error").getAsString());
    }

    @Test
    void whenMethodNotAllowed_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, resp.statusCode());
        assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Метод не поддерживается", error.get("error").getAsString());

        // Проверяем заголовок Allow
        String allow = resp.headers().firstValue("Allow").orElse("");
        assertTrue(allow.contains("GET"));
        assertTrue(allow.contains("POST"));
        assertTrue(allow.contains("DELETE"));
    }

    public static void assertContentType(HttpResponse<String> resp) {
        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);
    }

}