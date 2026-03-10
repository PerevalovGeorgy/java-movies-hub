package ru.practicum.moviehub.http;

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

public class DeleteMoviesApiTest {
    private static final String BASE = "http://localhost:8080"; // !!! добавьте базовую часть URL
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
    void deleteMovieById_whenExists_returns204() throws Exception {
        Movie movie = server.addMovie(new Movie("Начало", 2010));
        int id = movie.getId();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode());
        assertTrue(resp.headers().firstValue("Content-Type").isPresent());

    }

    @Test
    void deleteMovieById_whenNotExists_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        GetMoviesApiTest.assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Фильм не найден", error.get("error").getAsString());
    }

    @Test
    void deleteMovieById_whenIdNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        GetMoviesApiTest.assertContentType(resp);

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Некорректный ID", error.get("error").getAsString());
    }


}