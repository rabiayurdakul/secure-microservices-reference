package com.reference.catalogdataservice.controller;

import com.reference.catalogdataservice.dto.BookResponse;
import com.reference.catalogdataservice.dto.CreateBookRequest;
import com.reference.catalogdataservice.dto.PagedResponse;
import com.reference.catalogdataservice.entity.Book;
import com.reference.catalogdataservice.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;

    @Operation(
            summary = "Create a book",
            description = "Creates a book. Use Idempotency-Key header to safely retry requests.")
    @Parameter(
            name = "Idempotency-Key",
            in = ParameterIn.HEADER,
            required = false,
            description = "Unique key used to prevent duplicate book creation on retries")
    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        Book saved = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookResponse.from(saved));
    }

    @GetMapping
    public PagedResponse<BookResponse> listBooks(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String title,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<BookResponse> page = bookService.listBooks(author, title, pageable)
                                             .map(BookResponse::from);

        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @GetMapping("/{id}")
    public BookResponse getBook(@PathVariable UUID id) {
        return BookResponse.from(bookService.getBookById(id));
    }
}
