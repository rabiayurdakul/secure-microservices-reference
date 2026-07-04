package com.reference.catalogcacheservice.service;

import com.reference.catalogcacheservice.client.CatalogDataClient;
import com.reference.catalogcacheservice.response.BookResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookCacheService {

    private final RedisTemplate<String, BookResponse> redisTemplate;
    private final CatalogDataClient catalogDataClient;

    @Value("${catalog.cache.ttl-seconds}")
    private long ttlSeconds;

    public Optional<BookResponse> getBook(UUID id) {
        String cacheKey = buildKey(id);

        BookResponse cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT for book {}", id);
            return Optional.of(cached);
        }

        log.debug("Cache MISS for book {}", id);
        Optional<BookResponse> fromSource = catalogDataClient.getBookById(id);

        fromSource.ifPresent(book ->
                                     redisTemplate.opsForValue().set(cacheKey, book, Duration.ofSeconds(ttlSeconds)));

        return fromSource;
    }

    public void evict(UUID id) {
        redisTemplate.delete(buildKey(id));
        log.debug("Evicted cache for book {}", id);
    }

    private String buildKey(UUID id) {
        return "book:" + id;
    }
}
