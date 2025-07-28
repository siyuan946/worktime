package com.example.worktime.controller;

import com.example.worktime.model.UploadedFile;
import com.example.worktime.repository.UploadedFileRepository;
import com.example.worktime.repository.WorkRecordRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin
public class UploadedFileController {
    private final UploadedFileRepository repository;
    private final WorkRecordRepository recordRepository;

    public UploadedFileController(UploadedFileRepository repository, WorkRecordRepository recordRepository) {
        this.repository = repository;
        this.recordRepository = recordRepository;
    }

    @GetMapping
    public List<UploadedFile> all() {
        return repository.findAllWithRecords();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
