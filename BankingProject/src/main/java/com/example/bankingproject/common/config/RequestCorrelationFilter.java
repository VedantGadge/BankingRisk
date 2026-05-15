package com.example.bankingproject.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Injects a unique traceId into the MDC (Mapped Diagnostic Context) for every
 * incoming HTTP request. This traceId automatically appears in every log line
 * produced during that request — making it trivial to trace a single request
 * across hundreds of log lines in production.
 *
 * Also propagates X-Trace-Id in the response header so clients/API gateways
 * can correlate their own logs with the server's logs.
 */
@Component
@Order(1)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_KEY = "traceId";
    private static final String MDC_METHOD_KEY = "method";
    private static final String MDC_URI_KEY = "uri";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Use caller-supplied trace ID if provided (for distributed tracing),
            // otherwise generate a fresh one
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString();
            }

            MDC.put(MDC_TRACE_KEY, traceId);
            MDC.put(MDC_METHOD_KEY, request.getMethod());
            MDC.put(MDC_URI_KEY, request.getRequestURI());

            // Echo trace ID back so clients can correlate their request with server logs
            response.setHeader(TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);
        } finally {
            // Always clear MDC to prevent leaking data to the next request
            // on the same thread (thread pool reuse)
            MDC.clear();
        }
    }
}
