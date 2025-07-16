package com.example.worktime.repository;

import com.example.worktime.model.WorkRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkRecordRepository extends JpaRepository<WorkRecord, Long> {
    java.util.List<WorkRecord> findByBarcode(String barcode);

    java.util.List<WorkRecord> findByFileId(Long fileId);

    java.util.List<WorkRecord> findByFileIdAndFilledTrue(Long fileId);

    void deleteByFileId(Long fileId);
}
