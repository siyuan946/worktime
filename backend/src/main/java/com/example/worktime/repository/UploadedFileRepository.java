package com.example.worktime.repository;

import com.example.worktime.model.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    java.util.Optional<UploadedFile> findByFileName(String fileName);
}
