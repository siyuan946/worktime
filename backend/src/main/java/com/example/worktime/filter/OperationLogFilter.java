package com.example.worktime.filter;

import com.example.worktime.model.OperationLog;
import com.example.worktime.repository.OperationLogRepository;
import com.example.worktime.service.OperationLogContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class OperationLogFilter extends OncePerRequestFilter {
    private final OperationLogRepository repository;
    private final OperationLogContext context;

    public OperationLogFilter(OperationLogRepository repository, OperationLogContext context) {
        this.repository = repository;
        this.context = context;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        context.reset();
        String user = wrappedRequest.getHeader("X-User");
        long start = System.currentTimeMillis();
        String traceId = resolveTraceId(wrappedRequest);
        context.setTraceId(traceId);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            try {
                if (user != null && wrappedRequest.getAttribute("operationLogged") == null
                        && !wrappedRequest.getRequestURI().startsWith("/api/logs")) {
                    OperationLog log = new OperationLog();
                    log.setUsername(user);
                    log.setTimestamp(LocalDateTime.now());
                    log.setTraceId(traceId);
                    String path = wrappedRequest.getRequestURI().replaceFirst("^/api", "");
                    String query = wrappedRequest.getQueryString();
                    log.setPath(path);
                    log.setQuery(query);
                    log.setMethod(wrappedRequest.getMethod());
                    String action = wrappedRequest.getMethod() + " " + path + (query != null ? "?" + query : "");
                    log.setAction(action);
                    log.setStatusCode(wrappedResponse.getStatus());
                    long duration = System.currentTimeMillis() - start;
                    log.setDurationMs(duration);
                    log.setClientIp(resolveClientIp(wrappedRequest));
                    log.setUserAgent(wrappedRequest.getHeader("User-Agent"));
                    log.setDetails(buildDetails(wrappedRequest, wrappedResponse, duration));
                    context.apply(log);
                    repository.save(log);
                }
            } finally {
                wrappedResponse.copyBodyToResponse();
                context.reset();
            }
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader("X-Trace-Id");
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String client = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(client)) {
            client = request.getRemoteAddr();
        }
        if (client != null && client.contains(",")) {
            return client.split(",", 2)[0].trim();
        }
        return client;
    }

    private String buildDetails(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        StringBuilder builder = new StringBuilder();
        builder.append("Status: ").append(response.getStatus()).append('\n');
        builder.append("Duration: ").append(duration).append(" ms\n");
        String client = resolveClientIp(request);
        if (StringUtils.hasText(client)) {
            builder.append("Client: ").append(client).append('\n');
        }
        String userAgent = request.getHeader("User-Agent");
        if (StringUtils.hasText(userAgent)) {
            builder.append("User-Agent: ").append(userAgent).append('\n');
        }
        if (!request.getParameterMap().isEmpty()) {
            builder.append("Parameters:\n");
            request.getParameterMap().forEach((key, values) -> {
                builder.append("  ").append(key).append("=");
                if (values != null) {
                    builder.append(String.join(",", values));
                }
                builder.append('\n');
            });
        }
        String requestBody = toBodyString(request.getContentAsByteArray(), request.getCharacterEncoding());
        if (StringUtils.hasText(requestBody)) {
            builder.append("Request Body:\n").append(requestBody).append('\n');
        }
        String responseBody = toBodyString(response.getContentAsByteArray(), response.getCharacterEncoding());
        if (StringUtils.hasText(responseBody)) {
            builder.append("Response Body:\n").append(responseBody).append('\n');
        }
        return builder.toString().trim();
    }

    private String toBodyString(byte[] buf, String encoding) {
        if (buf == null || buf.length == 0) {
            return null;
        }
        int max = Math.min(buf.length, 2048);
        Charset charset = Charset.forName(encoding != null ? encoding : "UTF-8");
        String body = new String(buf, 0, max, charset);
        if (buf.length > max) {
            body += "\n...(truncated)";
        }
        return body.trim();
    }
}
