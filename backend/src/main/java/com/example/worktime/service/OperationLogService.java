package com.example.worktime.service;

import com.example.worktime.model.OperationLog;
import com.example.worktime.repository.OperationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
public class OperationLogService {
    private final OperationLogRepository repository;
    private final OperationLogContext context;

    public OperationLogService(OperationLogRepository repository, OperationLogContext context) {
        this.repository = repository;
        this.context = context;
    }

    public void log(String username, String action, String details) {
        if (username == null) return;
        OperationLog log = new OperationLog();
        log.setUsername(username);
        log.setAction(action);
        if (StringUtils.hasText(details)) {
            log.setDetails(details);
        }
        log.setTimestamp(LocalDateTime.now());
        context.apply(log);
        repository.save(log);
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            if (req != null) req.setAttribute("operationLogged", true);
        }
        context.reset();
    }
}
