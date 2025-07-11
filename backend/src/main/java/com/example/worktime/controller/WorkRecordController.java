package com.example.worktime.controller;

import com.example.worktime.model.WorkRecord;
import com.example.worktime.repository.WorkRecordRepository;
import com.example.worktime.service.ProcessCodeService;
import com.example.worktime.service.WorkerService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/workrecords")
@CrossOrigin
public class WorkRecordController {
    private final WorkRecordRepository repository;
    private final ProcessCodeService processService;
    private final WorkerService workerService;

    public WorkRecordController(WorkRecordRepository repository, ProcessCodeService processService, WorkerService workerService) {
        this.repository = repository;
        this.processService = processService;
        this.workerService = workerService;
    }

    @GetMapping
    public List<WorkRecord> all() {
        return repository.findAll();
    }

    @PostMapping
    public List<WorkRecord> save(@RequestBody List<WorkRecord> records) {
        for (WorkRecord r : records) {
            if (r.getWorkerCodes() != null) {
                String[] codes = r.getWorkerCodes().split("[,\u3001\s]+");
                List<String> names = new ArrayList<>();
                for (String c : codes) {
                    if (c.isBlank()) continue;
                    var w = workerService.getByCode(c.trim());
                    if (w != null) names.add(w.getName());
                }
                r.setWorkerNames(String.join(",", names));
            }
        }
        return repository.saveAll(records);
    }

    @PostMapping("/parse")
    public List<WorkRecord> parse(@RequestParam("file") MultipartFile file) throws IOException {
        List<WorkRecord> result = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 1) return result;
            Row header = sheet.getRow(0);
            Map<String, Integer> col = new HashMap<>();
            for (Cell cell : header) {
                col.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String notification = getString(row, col.get("产品代码"));
                String productName = getString(row, col.get("所属产品"));
                String drawing = getString(row, col.get("代号"));
                String partName = getString(row, col.get("名称"));
                Integer qty = getInt(row, col.get("产量"));

                int idx = 0;
                while (true) {
                    String pKey = idx==0 ? "工序" : "工序."+idx;
                    String hKey = idx==0 ? "工时" : "工时."+idx;
                    if (!col.containsKey(pKey) || !col.containsKey(hKey)) break;
                    String process = getString(row, col.get(pKey));
                    Double hours = getDouble(row, col.get(hKey));
                    if (process == null && hours == null) { idx++; continue; }
                    WorkRecord wr = new WorkRecord();
                    wr.setNotificationNumber(notification);
                    wr.setProductName(productName);
                    wr.setDrawingNumber(drawing);
                    wr.setPartName(partName);
                    wr.setPlanQty(qty);
                    wr.setProcessName(process);
                    wr.setProcessCode(processService.getCode(process));
                    wr.setHours(hours);
                    result.add(wr);
                    idx++;
                }
            }
        }
        return result;
    }

    private String getString(Row row, Integer idx) {
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long)c.getNumericCellValue());
        }
        return c.toString();
    }

    private Double getDouble(Row row, Integer idx) {
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC) return c.getNumericCellValue();
        try { return Double.parseDouble(c.toString()); } catch (Exception e) { return null; }
    }

    private Integer getInt(Row row, Integer idx) {
        Double d = getDouble(row, idx);
        return d == null ? null : d.intValue();
    }
}
