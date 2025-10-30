package com.example.worktime.controller;

import com.example.worktime.model.UploadedFile;
import com.example.worktime.repository.UploadedFileRepository;
import com.example.worktime.repository.WorkRecordRepository;
import com.example.worktime.service.OperationLogContext;
import com.example.worktime.service.OperationLogService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin
public class UploadedFileController {
    private final UploadedFileRepository repository;
    private final WorkRecordRepository recordRepository;
    private final OperationLogService logService;
    private final OperationLogContext logContext;

    public UploadedFileController(UploadedFileRepository repository, WorkRecordRepository recordRepository,
                                 OperationLogService logService, OperationLogContext logContext) {
        this.repository = repository;
        this.recordRepository = recordRepository;
        this.logService = logService;
        this.logContext = logContext;
    }

    @GetMapping
    public List<UploadedFile> all() {
        return repository.findAllWithRecords();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id, @RequestHeader("X-User") String user) {
        UploadedFile file = repository.findById(id).orElse(null);
        long cnt = recordRepository.countByFileId(id);
        recordRepository.deleteByFileId(id);
        repository.deleteById(id);
        String name = file != null ? file.getFileName() : String.valueOf(id);
        logContext.setModule("文件管理");
        logContext.setEntity("UploadedFile", id != null ? id.toString() : null);
        logContext.setSummary("删除上传文件");
        logContext.appendDetail("fileName=" + name);
        logService.log(user, "删除文件 " + name, "records=" + cnt);
    }
}
