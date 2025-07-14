package com.example.worktime.repository;

import com.example.worktime.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerRepository extends JpaRepository<Worker, Long> {
    Worker findByCode(String code);
}
