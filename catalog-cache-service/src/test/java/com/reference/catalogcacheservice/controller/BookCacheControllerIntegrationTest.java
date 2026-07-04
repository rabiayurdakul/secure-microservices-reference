package com.reference.catalogcacheservice.controller;

import com.reference.catalogcacheservice.client.CatalogDataClient;
import com.reference.catalogcacheservice.response.BookResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BookCacheControllerIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @MockitoBean
    private CatalogDataClient catalogDataClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, BookResponse> redisTemplate;

    private final UUID bookId = UUID.randomUUID();

    private final BookResponse bookResponse = new BookResponse(
            bookId,
            "Clean Code",
            "Robert Martin",
            "9780132350884",
            2008
    );

    @BeforeEach
    void cleanUp() {
        redisTemplate.delete("book:" + bookId);
    }

    @Test
    void getBook_cacheMiss_callsDataServiceAndCachesResult() throws Exception {
        when(catalogDataClient.getBookById(bookId))
                .thenReturn(Optional.of(bookResponse));

        mockMvc.perform(get("/api/v1/books/{id}", bookId)
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.title").value("Clean Code"))
               .andExpect(jsonPath("$.author").value("Robert Martin"));

        BookResponse cached = redisTemplate.opsForValue().get("book:" + bookId);

        assertThat(cached).isNotNull();
        assertThat(cached.title()).isEqualTo("Clean Code");

        verify(catalogDataClient, times(1)).getBookById(bookId);
    }

    @Test
    void getBook_cacheHit_doesNotCallDataService() throws Exception {
        redisTemplate.opsForValue()
                     .set("book:" + bookId, bookResponse, Duration.ofMinutes(5));

        mockMvc.perform(get("/api/v1/books/{id}", bookId)
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.title").value("Clean Code"));

        verifyNoInteractions(catalogDataClient);
    }

    @Test
    void getBook_whenNotFoundInDataService_returns404() throws Exception {
        when(catalogDataClient.getBookById(bookId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/books/{id}", bookId)
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotFound());
    }

    @Test
    void evictCache_removesFromRedis() throws Exception {
        redisTemplate.opsForValue()
                     .set("book:" + bookId, bookResponse, Duration.ofMinutes(5));

        mockMvc.perform(delete("/api/v1/books/{id}/cache", bookId))
               .andExpect(status().isNoContent());

        assertThat(redisTemplate.opsForValue().get("book:" + bookId)).isNull();
    }
}