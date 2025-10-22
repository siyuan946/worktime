package com.example.worktime.repository;

import com.example.worktime.model.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> {
    List<OperationLog> findByUsernameOrderByTimestampDesc(String username);
    List<OperationLog> findAllByOrderByTimestampDesc();
}
