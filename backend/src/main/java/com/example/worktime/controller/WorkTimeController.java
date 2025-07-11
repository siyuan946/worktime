package com.example.worktime.controller;

import com.example.worktime.model.WorkStep;
import com.example.worktime.model.WorkStepRecord;
import com.example.worktime.model.WorkTime;
import com.example.worktime.repository.WorkStepRecordRepository;
import com.example.worktime.repository.WorkStepRepository;
import com.example.worktime.repository.WorkTimeRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WorkTimeController {

    @Autowired
    private WorkTimeRepository workTimeRepository;
    @Autowired
    private WorkStepRepository workStepRepository;
    @Autowired
    private WorkStepRecordRepository workStepRecordRepository;

    @PostMapping("/worktime/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            Map<String, Integer> col = new HashMap<>();
            for (int i = 0; i < header.getPhysicalNumberOfCells(); i++) {
                col.put(header.getCell(i).getStringCellValue(), i);
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String stepName = row.getCell(col.getOrDefault("工序", 0)).getStringCellValue();
                int planned = (int) row.getCell(col.getOrDefault("产量", 1)).getNumericCellValue();
                WorkTime workTime = new WorkTime();
                workTime.setPlannedQty(planned);
                workTimeRepository.save(workTime);
                WorkStep step = new WorkStep();
                step.setName(stepName);
                step.setWorkTime(workTime);
                workStepRepository.save(step);
            }
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/worksteps/{id}/records")
    public ResponseEntity<?> addRecord(@PathVariable Long id, @RequestBody WorkStepRecord record) {
        WorkStep step = workStepRepository.findById(id).orElseThrow();
        record.setStep(step);
        if (record.getDoneDate() == null) {
            record.setDoneDate(LocalDate.now());
        }
        int done = workStepRecordRepository.sumQuantityByStep(step);
        workStepRecordRepository.save(record);
        boolean exceeded = false;
        WorkTime workTime = step.getWorkTime();
        if (workTime != null && workTime.getPlannedQty() != null) {
            exceeded = done + record.getQuantity() > workTime.getPlannedQty();
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("record", record);
        if (exceeded) {
            resp.put("message", "已超过计划产量");
        }
        return ResponseEntity.ok(resp);
    }
}
