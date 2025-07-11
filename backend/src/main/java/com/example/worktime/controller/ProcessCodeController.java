package com.example.worktime.controller;

import com.example.worktime.model.ProcessCode;
import com.example.worktime.repository.ProcessCodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/processcodes")
@CrossOrigin
public class ProcessCodeController {
    private final ProcessCodeRepository repository;

    public ProcessCodeController(ProcessCodeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProcessCode> all() {
        return repository.findAll();
    }

    @PostMapping
    public ProcessCode create(@RequestBody ProcessCode pc) {
        validate(pc);
        return repository.save(pc);
    }

    @PutMapping("/{id}")
    public ProcessCode update(@PathVariable Long id, @RequestBody ProcessCode pc) {
        pc.setId(id);
        validate(pc);
        return repository.save(pc);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    private void validate(ProcessCode pc) {
        if (pc.getCode() == null || pc.getCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Process code required");
        }
        if (pc.getName() == null || pc.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Process name required");
        }
    }
}
