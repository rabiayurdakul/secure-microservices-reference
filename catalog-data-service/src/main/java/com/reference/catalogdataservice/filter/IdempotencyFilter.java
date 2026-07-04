package com.reference.catalogdataservice.filter;

import com.reference.catalogdataservice.entity.IdempotencyKeyRecord;
import com.reference.catalogdataservice.repository.IdempotencyKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String idempotencyKey = request.getHeader("Idempotency-Key");

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<IdempotencyKeyRecord> existing = idempotencyKeyRepository.findById(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyKeyRecord record = existing.get();
            if (record.getResponseStatus() == null) {

                response.setStatus(HttpStatus.CONFLICT.value());
                response.getWriter().write("{\"error\": \"Request with this Idempotency-Key is already being processed\"}");
                return;
            }
            response.setHeader("Idempotent-Replayed", "true");
            response.setStatus(record.getResponseStatus());
            response.setContentType("application/json");
            response.getWriter().write(record.getResponseBody());
            return;
        }

        try {
            IdempotencyKeyRecord newRecord = new IdempotencyKeyRecord();
            newRecord.setIdempotencyKey(idempotencyKey);
            newRecord.setRequestPath(request.getRequestURI());
            idempotencyKeyRepository.save(newRecord);
        } catch (DataIntegrityViolationException e) {

            response.setStatus(HttpStatus.CONFLICT.value());
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrappedResponse);

        String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
        idempotencyKeyRepository.findById(idempotencyKey).ifPresent(record -> {
            record.setResponseStatus(wrappedResponse.getStatus());
            record.setResponseBody(responseBody);
            idempotencyKeyRepository.save(record);
        });

        wrappedResponse.copyBodyToResponse();
    }
}
