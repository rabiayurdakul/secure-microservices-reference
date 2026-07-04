package com.reference.catalogdataservice.controller;

import com.reference.catalogdataservice.AbstractIntegrationTest;
import com.reference.catalogdataservice.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class BookControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void cleanUp() {
        bookRepository.deleteAll();
    }

    @Test
    void getBooks_withoutAuth_isAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/books"))
               .andExpect(status().isOk());
    }

    @Test
    void createBook_withoutAuth_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Clean Code",
                                  "author": "Robert Martin",
                                  "isbn": "9780132350884",
                                  "publishedYear": 2008
                                }
                                """))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void createBook_withAuth_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                                .with(jwt())
                                .header("Idempotency-Key", "test-create-book-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Clean Code",
                                  "author": "Robert Martin",
                                  "isbn": "9780132350884",
                                  "publishedYear": 2008
                                }
                                """))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.title").value("Clean Code"))
               .andExpect(jsonPath("$.author").value("Robert Martin"))
               .andExpect(jsonPath("$.isbn").value("9780132350884"))
               .andExpect(jsonPath("$.publishedYear").value(2008));
    }

    @Test
    void createBook_withInvalidBody_returnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                                .with(jwt())
                                .header("Idempotency-Key", "test-invalid-book-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "author": "Robert Martin"
                                }
                                """))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    void getBook_whenNotExists_returnsProblemDetail() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/books/{id}", id)
                                .with(jwt()))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.title").value("Book Not Found"));
    }
}