package com.reference.catalogdataservice;

import com.reference.catalogdataservice.repository.BookRepository;
import com.reference.catalogdataservice.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class IdempotencyFilterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @BeforeEach
    void cleanUp() {
        idempotencyKeyRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    void duplicateRequestWithSameKey_doesNotCreateDuplicateBook() throws Exception {
        String body = "{\"title\":\"Clean Code\",\"author\":\"Robert Martin\",\"isbn\":\"9780132350884\",\"publishedYear\":2008}";

        mockMvc.perform(post("/api/v1/books")
                                .with(jwt())
                                .header("Idempotency-Key", "test-key-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
               .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/books")
                                .with(jwt())
                                .header("Idempotency-Key", "test-key-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
               .andExpect(status().isCreated())
               .andExpect(header().string("Idempotent-Replayed", "true"))
                                  .andExpect(jsonPath("$.title").value("Clean Code"))
                                  .andExpect(jsonPath("$.isbn").value("9780132350884"));

        assertThat(bookRepository.findAll()).hasSize(1);
    }
}
