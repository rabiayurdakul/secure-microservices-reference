CREATE TABLE books (
                       id UUID PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       author VARCHAR(255) NOT NULL,
                       isbn VARCHAR(20) UNIQUE,
                       published_year INTEGER,
                       created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_books_author ON books(author);