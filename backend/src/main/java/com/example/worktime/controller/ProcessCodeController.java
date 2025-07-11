package com.example.worktime.controller;

import com.example.worktime.model.ProcessCode;
import com.example.worktime.repository.ProcessCodeRepository;
import org.springframework.web.bind.annotation.*;

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
        return repository.save(pc);
    }

    @PutMapping("/{id}")
    public ProcessCode update(@PathVariable Long id, @RequestBody ProcessCode pc) {
        pc.setId(id);
        return repository.save(pc);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
