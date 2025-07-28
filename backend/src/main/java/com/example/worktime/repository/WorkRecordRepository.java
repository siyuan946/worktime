package com.example.worktime.repository;

import com.example.worktime.model.WorkRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkRecordRepository extends JpaRepository<WorkRecord, Long> {
    java.util.List<WorkRecord> findByBarcode(String barcode);

    java.util.List<WorkRecord> findByFileId(Long fileId);

    java.util.List<WorkRecord> findByFileIdAndFilledTrue(Long fileId);

    @org.springframework.data.jpa.repository.Query("select r from WorkRecord r where r.file.uploadTime >= :start and r.file.uploadTime < :end and r.filled = true")
    java.util.List<WorkRecord> findByUploadDate(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
                                                @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    void deleteByFileId(Long fileId);
}
