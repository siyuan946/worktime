package com.example.worktime.controller;

import com.example.worktime.model.WorkTime;
import com.example.worktime.repository.WorkTimeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/worktimes")
@CrossOrigin
public class WorkTimeController {

    private final WorkTimeRepository repository;

    public WorkTimeController(WorkTimeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<WorkTime> findAll() {
        return repository.findAll();
    }

    @PostMapping
    public WorkTime create(@RequestBody WorkTime workTime) {
        return repository.save(workTime);
    }
}
