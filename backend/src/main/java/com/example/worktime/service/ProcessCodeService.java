package com.example.worktime.service;

import com.example.worktime.model.ProcessCode;
import com.example.worktime.repository.ProcessCodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service converting process names to codes using the database table.
 */
@Service
public class ProcessCodeService {

    private final ProcessCodeRepository repository;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public ProcessCodeService(ProcessCodeRepository repository) {
        this.repository = repository;
    }

    /**
     * Pre-load all known process name/code pairs into the local cache and
     * return a mutable copy that callers can reuse when resolving names in bulk.
     */
    public Map<String, String> loadCacheSnapshot() {
        List<ProcessCode> all = repository.findAll();
        Map<String, String> snapshot = new HashMap<>();
        for (ProcessCode pc : all) {
            if (pc.getName() == null || pc.getCode() == null) {
                continue;
            }
            String name = pc.getName().trim();
            String code = pc.getCode().trim();
            if (name.isEmpty() || code.isEmpty()) {
                continue;
            }
            cache.put(name, code);
            snapshot.put(name, code);
        }
        return snapshot;
    }

    public Map<String, String> getCachedMappings() {
        return Collections.unmodifiableMap(cache);
    }

    public String getCode(String processName) {
        if (processName == null) return null;
        String name = processName.trim();
        if (name.isEmpty()) return null;
        String existing = cache.get(name);
        if (existing != null) {
            return existing;
        }
        ProcessCode pc = repository.findByName(name);
        if (pc != null && pc.getCode() != null) {
            String code = pc.getCode().trim();
            if (!code.isEmpty()) {
                cache.put(name, code);
                return code;
            }
        }
        return null;
    }

    public Map<String, String> getCodes(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        List<String> pending = new java.util.ArrayList<>();
        for (String raw : names) {
            if (raw == null) {
                continue;
            }
            String name = raw.trim();
            if (name.isEmpty()) {
                continue;
            }
            String cached = cache.get(name);
            if (cached != null) {
                result.put(name, cached);
            } else {
                pending.add(name);
            }
        }
        if (!pending.isEmpty()) {
            List<ProcessCode> found = repository.findByNameIn(pending);
            for (ProcessCode pc : found) {
                if (pc == null || pc.getName() == null || pc.getCode() == null) {
                    continue;
                }
                String name = pc.getName().trim();
                String code = pc.getCode().trim();
                if (name.isEmpty() || code.isEmpty()) {
                    continue;
                }
                cache.put(name, code);
                result.put(name, code);
            }
        }
        return result;
    }

    public ProcessCode rememberMapping(String name, String code) {
        if (name == null || code == null) {
            return null;
        }
        String trimmedName = name.trim();
        String trimmedCode = code.trim();
        if (trimmedName.isEmpty() || trimmedCode.isEmpty()) {
            return null;
        }
        ProcessCode existing = repository.findByName(trimmedName);
        ProcessCode duplicate = repository.findByCode(trimmedCode);
        if (duplicate != null && (existing == null || !duplicate.getId().equals(existing.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工序代号已存在，请使用其他代号");
        }
        ProcessCode target;
        boolean created = false;
        if (existing != null) {
            existing.setCode(trimmedCode);
            target = existing;
        } else {
            ProcessCode fresh = new ProcessCode();
            fresh.setName(trimmedName);
            fresh.setCode(trimmedCode);
            target = fresh;
            created = true;
        }
        ProcessCode saved = repository.save(target);
        cache.put(trimmedName, trimmedCode);
        if (!created && saved.getName() != null && !saved.getName().trim().isEmpty()) {
            cache.put(saved.getName().trim(), trimmedCode);
        }
        return saved;
    }
}
