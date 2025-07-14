package com.example.worktime.controller;

import com.example.worktime.model.UploadedFile;
import com.example.worktime.repository.UploadedFileRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin
public class UploadedFileController {
    private final UploadedFileRepository repository;

    public UploadedFileController(UploadedFileRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<UploadedFile> all() {
        return repository.findAll();
    }
}
