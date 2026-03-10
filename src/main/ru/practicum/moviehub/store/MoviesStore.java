package ru.practicum.moviehub.store;

import ru.practicum.moviehub.store.Movie;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new ConcurrentHashMap<>();
    private int nextId = 1;


    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies.values());
    }

    public List<Movie> getMoviesByYear(int year) {
        return movies.values().stream()
                .filter(movie -> movie.getYear() != null && movie.getYear() == year)
                .collect(Collectors.toList());
    }

    public Optional<Movie> getMovieById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public Movie addMovie(Movie movie) {
        movie.setId(nextId++);
        movies.put(movie.getId(), movie);
        return movie;
    }

    public Optional<Movie> updateMovie(int id, Movie movie) {
        if (movies.containsKey(id)) {
            movie.setId(id);
            movies.put(id, movie);
            return Optional.of(movie);
        }
        return Optional.empty();
    }

    public boolean deleteMovie(int id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }
}