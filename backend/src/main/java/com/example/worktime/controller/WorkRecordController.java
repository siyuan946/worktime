package com.example.worktime.controller;

import com.example.worktime.model.WorkRecord;
import com.example.worktime.model.UploadedFile;
import com.example.worktime.repository.WorkRecordRepository;
import com.example.worktime.repository.UploadedFileRepository;
import com.example.worktime.service.ProcessCodeService;
import com.example.worktime.service.WorkerService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/workrecords")
@CrossOrigin
public class WorkRecordController {
    private final WorkRecordRepository repository;
    private final ProcessCodeService processService;
    private final WorkerService workerService;
    private final UploadedFileRepository fileRepository;

    public WorkRecordController(WorkRecordRepository repository, ProcessCodeService processService,
                                WorkerService workerService, UploadedFileRepository fileRepository) {
        this.repository = repository;
        this.processService = processService;
        this.workerService = workerService;
        this.fileRepository = fileRepository;
    }

    @GetMapping
    public List<WorkRecord> all() {
        return repository.findAll();
    }

    @GetMapping("/barcode/{barcode}")
    public List<WorkRecord> byBarcode(@PathVariable String barcode) {
        return repository.findByBarcode(barcode);
    }

    @PutMapping("/{id}")
    public WorkRecord update(@PathVariable Long id, @RequestBody WorkRecord record) {
        record.setId(id);
        prepare(record);
        return repository.save(record);
    }

    @PostMapping
    public List<WorkRecord> save(@RequestParam("fileId") Long fileId, @RequestBody List<WorkRecord> records) {
        UploadedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效文件"));
        for (WorkRecord r : records) {
            r.setFile(file);
            r.setSupplemental(!repository.findByBarcode(r.getBarcode()).isEmpty());
            prepare(r);
        }
        return repository.saveAll(records);
    }

    @PostMapping("/parse")
    public Map<String, Object> parse(@RequestParam("file") MultipartFile file) throws IOException {
        UploadedFile uf = new UploadedFile();
        uf.setFileName(file.getOriginalFilename());
        uf.setData(file.getBytes());
        uf.setUploadTime(java.time.LocalDateTime.now());
        uf = fileRepository.save(uf);

        List<WorkRecord> records = parseExcel(file);

        Map<String, Object> result = new HashMap<>();
        result.put("fileId", uf.getId());
        result.put("records", records);
        return result;
    }

    private List<WorkRecord> parseExcel(MultipartFile file) throws IOException {
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
                String productCode = getString(row, col.get("产品代码"));
                String productName = getString(row, col.get("所属产品"));
                String drawing = getString(row, col.get("代号"));
                String partName = getString(row, col.get("名称"));
                Integer qty = getInt(row, col.get("产量"));

                int idx = 0;
                while (true) {
                    String pKey = idx == 0 ? "工序" : "工序." + idx;
                    String hKey = idx == 0 ? "工时" : "工时." + idx;
                    if (!col.containsKey(pKey) || !col.containsKey(hKey)) break;
                    String process = getString(row, col.get(pKey));
                    Double hours = getDouble(row, col.get(hKey));
                    if (process == null && hours == null) { idx++; continue; }
                    WorkRecord wr = new WorkRecord();
                    wr.setNotificationNumber(productCode);
                    wr.setProductCode(productCode);
                    wr.setProductName(productName);
                    wr.setDrawingNumber(drawing);
                    wr.setPartName(partName);
                    wr.setPlanQty(qty);
                    wr.setProcessName(process);
                    String code = processService.getCode(process);
                    wr.setProcessCode(code);
                    if (drawing != null && productCode != null && code != null) {
                        wr.setBarcode(drawing + "-" + productCode + "-" + code);
                    }
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

    private void prepare(WorkRecord record) {
        validate(record);
        if (record.getWorkerCodes() != null) {
            String[] codes = record.getWorkerCodes().split("[,\u3001\s]+");
            List<String> names = new ArrayList<>();
            for (String c : codes) {
                if (c.isBlank()) continue;
                var w = workerService.getByCode(c.trim());
                if (w != null) names.add(w.getName());
            }
            record.setWorkerNames(String.join(",", names));
        }
        if (record.getQualifiedQty() != null && record.getHours() != null) {
            record.setHourSubtotal(record.getQualifiedQty() * record.getHours());
        }
    }

    private void validate(WorkRecord record) {
        if (record.getProcessCode() == null || record.getProcessCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工序代码不能为空");
        }
        if (record.getBarcode() == null || record.getBarcode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "条形码不能为空");
        }
    }
}
