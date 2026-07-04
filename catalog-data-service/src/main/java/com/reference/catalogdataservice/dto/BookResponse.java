package com.reference.catalogdataservice.dto;

import com.reference.catalogdataservice.entity.Book;

import java.util.UUID;

public record BookResponse(UUID id, String title, String author, String isbn, Integer publishedYear) {

    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(), book.getPublishedYear());
    }
}


