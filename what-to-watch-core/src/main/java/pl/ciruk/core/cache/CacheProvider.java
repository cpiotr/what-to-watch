package pl.ciruk.core.cache;

import java.util.Optional;

public interface CacheProvider<T> {

    void put(String key, T value);

    Optional<T> get(String key);

    static <T> CacheProvider<T> empty() {
        return new CacheProvider<T>() {
            @Override
            public void put(String key, T value) {

            }

            @Override
            public Optional<T> get(String key) {
                return Optional.empty();
            }
        };
    }
}
