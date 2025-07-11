package com.example.worktime.repository;

import com.example.worktime.model.WorkStep;
import com.example.worktime.model.WorkStepRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkStepRecordRepository extends JpaRepository<WorkStepRecord, Long> {
    @Query("select coalesce(sum(r.quantity),0) from WorkStepRecord r where r.step = :step")
    int sumQuantityByStep(@Param("step") WorkStep step);
}
