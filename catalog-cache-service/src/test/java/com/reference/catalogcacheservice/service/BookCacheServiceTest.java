package com.reference.catalogcacheservice.service;

import com.reference.catalogcacheservice.client.CatalogDataClient;
import com.reference.catalogcacheservice.response.BookResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookCacheServiceTest {

    @Mock
    private RedisTemplate<String, BookResponse> redisTemplate;

    @Mock
    private ValueOperations<String, BookResponse> valueOperations;

    @Mock
    private CatalogDataClient catalogDataClient;

    @InjectMocks
    private BookCacheService bookCacheService;

    private final UUID bookId = UUID.randomUUID();

    private final BookResponse bookResponse = new BookResponse(
            bookId,
            "Clean Code",
            "Robert Martin",
            "9780132350884",
            2008
    );

    @Test
    void getBook_whenCacheHit_returnsFromRedis_andDoesNotCallDataService() {
        ReflectionTestUtils.setField(bookCacheService, "ttlSeconds", 300L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("book:" + bookId)).thenReturn(bookResponse);

        Optional<BookResponse> result = bookCacheService.getBook(bookId);

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("Clean Code");

        verifyNoInteractions(catalogDataClient);
    }

    @Test
    void getBook_whenCacheMiss_callsDataService_andWritesToRedis() {
        ReflectionTestUtils.setField(bookCacheService, "ttlSeconds", 300L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("book:" + bookId)).thenReturn(null);
        when(catalogDataClient.getBookById(bookId)).thenReturn(Optional.of(bookResponse));

        Optional<BookResponse> result = bookCacheService.getBook(bookId);

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("Clean Code");

        verify(catalogDataClient).getBookById(bookId);
        verify(valueOperations).set(
                eq("book:" + bookId),
                eq(bookResponse),
                eq(Duration.ofSeconds(300))
        );
    }

    @Test
    void getBook_whenCacheMiss_andDataServiceReturnsEmpty_returnsEmpty() {
        ReflectionTestUtils.setField(bookCacheService, "ttlSeconds", 300L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("book:" + bookId)).thenReturn(null);
        when(catalogDataClient.getBookById(bookId)).thenReturn(Optional.empty());

        Optional<BookResponse> result = bookCacheService.getBook(bookId);

        assertThat(result).isEmpty();

        verify(catalogDataClient).getBookById(bookId);
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void evict_deletesFromRedis() {
        bookCacheService.evict(bookId);

        verify(redisTemplate).delete("book:" + bookId);
    }
}