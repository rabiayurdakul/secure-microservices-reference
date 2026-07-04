package com.reference.catalogcacheservice.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookResponse(UUID id, String title, String author, String isbn, Integer publishedYear) { }


