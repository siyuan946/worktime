package com.example.worktime.controller;

import com.example.worktime.model.Worker;
import com.example.worktime.repository.WorkerRepository;
import org.springframework.web.bind.annotation.*;

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
        return repository.save(worker);
    }

    @PutMapping("/{id}")
    public Worker update(@PathVariable Long id, @RequestBody Worker worker) {
        worker.setId(id);
        return repository.save(worker);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
