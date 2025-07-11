package com.example.worktime.repository;

import com.example.worktime.model.WorkRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkRecordRepository extends JpaRepository<WorkRecord, Long> {
}
