package com.example.worktime.filter;

import com.example.worktime.model.OperationLog;
import com.example.worktime.repository.OperationLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class OperationLogFilter extends OncePerRequestFilter {
    private final OperationLogRepository repository;

    public OperationLogFilter(OperationLogRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String user = request.getHeader("X-User");
        long start = System.currentTimeMillis();
        filterChain.doFilter(request, response);
        if (user != null && request.getAttribute("operationLogged") == null
                && !request.getRequestURI().startsWith("/api/logs")) {
            OperationLog log = new OperationLog();
            log.setUsername(user);
            String path = request.getRequestURI().replaceFirst("^/api", "");
            String query = request.getQueryString();
            String action = request.getMethod() + " " + path + (query != null ? "?" + query : "");
            log.setAction(action);
            log.setTimestamp(LocalDateTime.now());
            log.setDetails(buildDetails(request, response, start));
            repository.save(log);
        }
    }

    private String buildDetails(HttpServletRequest request, HttpServletResponse response, long start) {
        StringBuilder builder = new StringBuilder();
        builder.append("Status: ").append(response.getStatus()).append('\n');
        builder.append("Duration: ").append(System.currentTimeMillis() - start).append(" ms\n");
        String client = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(client)) {
            client = request.getRemoteAddr();
        }
        if (client != null) {
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
        return builder.toString().trim();
    }
}
