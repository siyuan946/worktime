package com.example.worktime.controller;

import com.example.worktime.model.OperationLog;
import com.example.worktime.repository.OperationLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin
public class OperationLogController {
    private final OperationLogRepository repository;

    public OperationLogController(OperationLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Page<OperationLog> logs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        int pageIndex = Math.max(page, 0);
        int pageSize = Math.max(1, Math.min(size, 200));
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "timestamp"));

        Specification<OperationLog> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (username != null && !username.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("username")), username.trim().toLowerCase()));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("action")), like),
                        cb.like(cb.lower(root.get("details")), like)
                ));
            }
            if (startDate != null) {
                LocalDateTime start = startDate.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), start));
            }
            if (endDate != null) {
                LocalDateTime end = endDate.plusDays(1).atStartOfDay();
                predicates.add(cb.lessThan(root.get("timestamp"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(specification, pageable);
    }
}
