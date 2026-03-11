package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.store.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GetMoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore;
    private static final Gson gson = new Gson();
    private static final ListOfMoviesTypeToken MOVIE_LIST_TYPE = new ListOfMoviesTypeToken();
    private static final int CURRENT_YEAR = Year.now().getValue();
    private static final int MAX_YEAR = CURRENT_YEAR + 1;

    @BeforeAll
    static void beforeAll() {
        moviesStore = new MoviesStore();
        server = new MoviesServer(moviesStore, 8080);
        client = HttpClient.newBuilder()
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
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        List<Movie> movies = gson.fromJson(resp.body(), MOVIE_LIST_TYPE);
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_whenHasMovies_returnsMoviesArray() throws Exception {
        Movie movie1 = moviesStore.addMovie(new Movie("Начало", 2010));
        Movie movie2 = moviesStore.addMovie(new Movie("Матрица", 1999));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        List<Movie> movies = gson.fromJson(resp.body(), MOVIE_LIST_TYPE);

        assertEquals(2, movies.size());

        assertTrue(movies.stream().anyMatch(m ->
                m.getId().equals(movie1.getId()) &&
                        m.getTitle().equals("Начало") &&
                        m.getYear() == 2010
        ));

        assertTrue(movies.stream().anyMatch(m ->
                m.getId().equals(movie2.getId()) &&
                        m.getTitle().equals("Матрица") &&
                        m.getYear() == 1999
        ));
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie addedMovie = moviesStore.addMovie(new Movie("Начало", 2010));
        int id = addedMovie.getId();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        Movie movie = gson.fromJson(resp.body(), Movie.class);

        assertEquals(addedMovie.getId(), movie.getId());
        assertEquals("Начало", movie.getTitle());
        assertEquals(2010, movie.getYear());
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
        moviesStore.addMovie(new Movie("Начало", 2010));
        moviesStore.addMovie(new Movie("Матрица", 1999));
        moviesStore.addMovie(new Movie("Интерстеллар", 2010));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2010"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        List<Movie> movies = gson.fromJson(resp.body(), MOVIE_LIST_TYPE);

        assertEquals(2, movies.size());

        assertTrue(movies.stream().allMatch(m -> m.getYear() == 2010));
    }

    @Test
    void getMoviesByYear_whenNoMovies_returnsEmptyArray() throws Exception {
        moviesStore.addMovie(new Movie("Начало", 2010));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2020"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        List<Movie> movies = gson.fromJson(resp.body(), MOVIE_LIST_TYPE);

        assertTrue(movies.isEmpty(), "Список фильмов должен быть пустым");
        assertEquals(0, movies.size(), "Размер списка должен быть 0");
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