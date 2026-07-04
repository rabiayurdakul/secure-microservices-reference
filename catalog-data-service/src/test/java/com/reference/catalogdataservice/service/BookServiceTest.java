package com.reference.catalogdataservice.service;

import com.reference.catalogdataservice.dto.BookResponse;
import com.reference.catalogdataservice.dto.CreateBookRequest;
import com.reference.catalogdataservice.entity.Book;
import com.reference.catalogdataservice.exception.BookNotFoundException;
import com.reference.catalogdataservice.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void getBook_whenNotFound_throwsException() {
        UUID id = UUID.randomUUID();

        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.getBookById(id));

        verify(bookRepository).findById(id);
    }

    @Test
    void createBook_savesAndReturnsBook() {
        CreateBookRequest request = new CreateBookRequest(
                "Clean Code",
                "Robert Martin",
                "9780132350884",
                2008
        );

        Book savedBook = new Book();
        savedBook.setId(UUID.randomUUID());
        savedBook.setTitle(request.title());
        savedBook.setAuthor(request.author());
        savedBook.setIsbn(request.isbn());
        savedBook.setPublishedYear(request.publishedYear());
        savedBook.setCreatedAt(Instant.now());

        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        Book result = bookService.createBook(request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getAuthor()).isEqualTo("Robert Martin");
        assertThat(result.getIsbn()).isEqualTo("9780132350884");
        assertThat(result.getPublishedYear()).isEqualTo(2008);
        assertThat(result.getCreatedAt()).isNotNull();

        verify(bookRepository).save(any(Book.class));
    }
}