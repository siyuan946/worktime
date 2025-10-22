package com.example.worktime.controller;

import com.example.worktime.model.ProcessCode;
import com.example.worktime.repository.ProcessCodeRepository;
import com.example.worktime.service.OperationLogService;
import com.example.worktime.service.ProcessCodeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/processcodes")
@CrossOrigin
public class ProcessCodeController {
    private final ProcessCodeRepository repository;
    private final OperationLogService logService;
    private final ProcessCodeService processService;

    public ProcessCodeController(ProcessCodeRepository repository,
                                 OperationLogService logService,
                                 ProcessCodeService processService) {
        this.repository = repository;
        this.logService = logService;
        this.processService = processService;
    }

    @GetMapping
    public List<ProcessCode> all() {
        return repository.findAll();
    }

    @GetMapping("/search")
    public List<ProcessCode> search(@RequestParam String term) {
        String q = term.trim();
        if (q.isEmpty()) {
            return repository.findAll();
        }
        return repository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(q, q);
    }

    @GetMapping("/name/{name}")
    public ProcessCode byName(@PathVariable String name) {
        return repository.findByName(name);
    }

    @PostMapping("/lookup")
    public Map<String, String> lookup(@RequestBody List<String> names) {
        return processService.getCodes(names);
    }

    @PostMapping("/remember")
    public ProcessCode remember(@RequestBody Map<String, String> payload, @RequestHeader("X-User") String user) {
        String name = payload != null ? payload.get("name") : null;
        String code = payload != null ? payload.get("code") : null;
        if (name == null || name.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工序名称和工序代号均不能为空");
        }
        String trimmedName = name.trim();
        String trimmedCode = code.trim();
        ProcessCode existing = repository.findByName(trimmedName);
        ensureUniqueCode(trimmedCode, existing != null ? existing.getId() : null);
        ProcessCode saved = processService.rememberMapping(trimmedName, trimmedCode);
        if (saved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工序名称和工序代号均不能为空");
        }
        if (existing == null) {
            logService.log(user, "新增工序 " + saved.getName(), "id=" + saved.getId());
        } else {
            logService.log(user, "更新工序 " + existing.getId(), null);
        }
        return saved;
    }

    @PostMapping
    public ProcessCode create(@RequestBody ProcessCode pc, @RequestHeader("X-User") String user) {
        validate(pc);
        ProcessCode saved = repository.save(pc);
        logService.log(user, "新增工序 " + pc.getName(), "id=" + saved.getId());
        return saved;
    }

    @PutMapping("/{id}")
    public ProcessCode update(@PathVariable Long id, @RequestBody ProcessCode pc, @RequestHeader("X-User") String user) {
        pc.setId(id);
        validate(pc);
        ProcessCode saved = repository.save(pc);
        logService.log(user, "更新工序 " + id, null);
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestHeader("X-User") String user) {
        repository.deleteById(id);
        logService.log(user, "删除工序 " + id, null);
    }

    private void validate(ProcessCode pc) {
        String code = pc.getCode() != null ? pc.getCode().trim() : null;
        if (code == null || code.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工序代号不能为空");
        }
        String name = pc.getName() != null ? pc.getName().trim() : null;
        if (name == null || name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工序名称不能为空");
        }
        pc.setCode(code);
        pc.setName(name);
        ensureUniqueCode(code, pc.getId());
    }

    private void ensureUniqueCode(String code, Long currentId) {
        if (code == null || code.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工序代号不能为空");
        }
        String trimmed = code.trim();
        ProcessCode duplicate = repository.findByCode(trimmed);
        if (duplicate != null && (currentId == null || !duplicate.getId().equals(currentId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工序代号已存在，请使用其他代号");
        }
    }
}
