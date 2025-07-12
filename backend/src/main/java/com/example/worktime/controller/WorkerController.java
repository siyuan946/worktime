package com.example.worktime.controller;

import com.example.worktime.model.Worker;
import com.example.worktime.repository.WorkerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
@CrossOrigin
public class WorkerController {
    private final WorkerRepository repository;

    public WorkerController(WorkerRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Worker> all() {
        return repository.findAll();
    }

    @PostMapping
    public Worker create(@RequestBody Worker worker) {
        validate(worker);
        return repository.save(worker);
    }

    @PutMapping("/{id}")
    public Worker update(@PathVariable Long id, @RequestBody Worker worker) {
        worker.setId(id);
        validate(worker);
        return repository.save(worker);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    private void validate(Worker worker) {
        if (worker.getCode() == null || worker.getCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工号不能为空");
        }
        if (worker.getName() == null || worker.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "姓名不能为空");
        }
    }
}
