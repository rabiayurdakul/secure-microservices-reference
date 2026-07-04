package com.reference.catalogdataservice.dto;


import jakarta.validation.constraints.NotBlank;

public record CreateBookRequest(@NotBlank String title,
                                @NotBlank String author,
                                String isbn,
                                Integer publishedYear) {}