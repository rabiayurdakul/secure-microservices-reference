package com.reference.catalogcacheservice.controller;

import com.reference.catalogcacheservice.exception.BookNotFoundException;
import com.reference.catalogcacheservice.response.BookResponse;
import com.reference.catalogcacheservice.service.BookCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookCacheController {

    private final BookCacheService bookCacheService;

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBook(@PathVariable UUID id) {
        return bookCacheService.getBook(id)
                               .map(ResponseEntity::ok)
                               .orElseThrow(() -> new BookNotFoundException(id));
    }

    @DeleteMapping("/{id}/cache")
    public ResponseEntity<Void> evictCache(@PathVariable UUID id) {
        bookCacheService.evict(id);
        return ResponseEntity.noContent().build();
    }
}
