package com.reference.catalogcacheservice.client;

import com.reference.catalogcacheservice.response.BookResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

@Component
public class CatalogDataClient {

    private final RestClient restClient;

    public CatalogDataClient(@Value("${catalog.data-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Optional<BookResponse> getBookById(UUID id) {
        try {
            BookResponse response = restClient.get()
                                              .uri("/api/v1/books/{id}", id)
                                              .retrieve()
                                              .body(BookResponse.class);
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
