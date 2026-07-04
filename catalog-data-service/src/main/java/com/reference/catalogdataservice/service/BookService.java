package com.reference.catalogdataservice.service;

import com.reference.catalogdataservice.dto.CreateBookRequest;
import com.reference.catalogdataservice.entity.Book;
import com.reference.catalogdataservice.exception.BookNotFoundException;
import com.reference.catalogdataservice.repository.BookRepository;
import com.reference.catalogdataservice.specification.BookSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;

    @Transactional
    public Book createBook(CreateBookRequest request) {
        Book book = new Book();
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setPublishedYear(request.publishedYear());

        return bookRepository.save(book);
    }

    @Transactional(readOnly = true)
    public Book getBookById(UUID id) {
        return bookRepository.findById(id)
                             .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Book> listBooks(String author, String title, Pageable pageable) {
        Specification<Book> spec = Specification.where(BookSpecifications.hasAuthor(author))
                                                .and(BookSpecifications.titleContains(title));
        return bookRepository.findAll(spec, pageable);
    }
    }

