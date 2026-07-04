package com.reference.catalogdataservice.specification;

import com.reference.catalogdataservice.entity.Book;
import org.springframework.data.jpa.domain.Specification;

public class BookSpecifications {

    public static Specification<Book> hasAuthor(String author) {
        return (root, query, cb) -> author == null ? null : cb.equal(root.get("author"), author);
    }

    public static Specification<Book> titleContains(String title) {
        return (root, query, cb) -> title == null ? null
                : cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }
}
