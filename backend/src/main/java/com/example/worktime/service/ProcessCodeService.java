package com.example.worktime.service;

import com.example.worktime.repository.ProcessCodeRepository;
import org.springframework.stereotype.Service;

/**
 * Service converting process names to codes using the database table.
 */
@Service
public class ProcessCodeService {

    private final ProcessCodeRepository repository;

    public ProcessCodeService(ProcessCodeRepository repository) {
        this.repository = repository;
    }

    public String getCode(String processName) {
        if (processName == null) return null;
        var pc = repository.findByName(processName);
        return pc != null ? pc.getCode() : null;
    }
}
