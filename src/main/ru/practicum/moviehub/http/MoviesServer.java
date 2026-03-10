package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;
import ru.practicum.moviehub.store.Movie;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesStore moviesStore;

    public MoviesServer(MoviesStore moviesStore, Integer port) {
        this.moviesStore = moviesStore;
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.createContext("/movies", new MoviesHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер на localhost:" + port, e);
        }
    }

    public Movie addMovie(Movie movie) {
        return moviesStore.addMovie(movie);
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен на http://localhost:" + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }
}