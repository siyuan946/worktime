package com.example.worktime.controller;

import com.example.worktime.model.WorkRecord;
import com.example.worktime.model.UploadedFile;
import com.example.worktime.repository.WorkRecordRepository;
import com.example.worktime.repository.UploadedFileRepository;
import com.example.worktime.service.ProcessCodeService;
import com.example.worktime.service.WorkerService;
import org.springframework.transaction.annotation.Transactional;
import com.example.worktime.model.Worker;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
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
        String clean = sanitizeBarcode(barcode);
        return repository.findByBarcode(clean);
    }

    @GetMapping("/file/{fileId}")
    public List<WorkRecord> byFile(@PathVariable Long fileId) {
        return repository.findByFileId(fileId);
    }

    @GetMapping("/generateBarcode")
    public String generateBarcodeEndpoint(@RequestParam("text") String text) {
        byte[] img = generateBarcode(sanitizeBarcode(text));
        return img == null ? null : java.util.Base64.getEncoder().encodeToString(img);
    }

    @PutMapping("/{id}")
    @Transactional
    public WorkRecord update(@PathVariable Long id, @RequestBody WorkRecord record) {
        WorkRecord existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "record not found"));
        record.setId(id);
        if (record.getFile() == null) {
            record.setFile(existing.getFile());
        }
        if (record.getBarcode() == null) record.setBarcode(existing.getBarcode());
        if (record.getBarcodeImage() == null) record.setBarcodeImage(existing.getBarcodeImage());
        prepare(record);
        return repository.save(record);
    }

    @PostMapping("/duplicate/{id}")
    public WorkRecord duplicate(@PathVariable Long id) {
        WorkRecord src = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "record not found"));
        WorkRecord copy = new WorkRecord();
        copy.setNotificationNumber(src.getNotificationNumber());
        copy.setProductName(src.getProductName());
        copy.setDrawingNumber(src.getDrawingNumber());
        copy.setProductCode(src.getProductCode());
        copy.setPartName(src.getPartName());
        copy.setPlanQty(src.getPlanQty());
        copy.setProcessName(src.getProcessName());
        copy.setProcessCode(src.getProcessCode());
        copy.setBarcode(src.getBarcode());
        copy.setBarcodeImage(src.getBarcodeImage());
        copy.setBatchNumber(src.getBatchNumber());
        copy.setHours(src.getHours());
        copy.setFile(src.getFile());
        copy.setSupplemental(true);
        prepare(copy);
        return repository.save(copy);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public List<WorkRecord> save(@RequestParam("fileId") Long fileId, @RequestBody List<WorkRecord> records) {
        UploadedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效文件"));
        for (WorkRecord r : records) {
            r.setFile(file);
            r.setSupplemental(!repository.findByBarcode(r.getBarcode()).isEmpty());
            prepare(r);
        }
        java.util.List<WorkRecord> saved = repository.saveAll(records);
        repository.flush();
        System.out.println("Saved records: " + saved.size());
        return saved;
    }

    @PostMapping("/parse")
    @Transactional
    public Map<String, Object> parse(@RequestParam("file") MultipartFile file) throws IOException {
        UploadedFile uf = new UploadedFile();
        uf.setFileName(file.getOriginalFilename());
        uf.setData(file.getBytes());
        uf.setUploadTime(java.time.LocalDateTime.now());
        uf = fileRepository.saveAndFlush(uf);

        List<WorkRecord> records = parseExcel(file);

        Map<String, Object> result = new HashMap<>();
        result.put("fileId", uf.getId());
        result.put("records", records);
        System.out.println("Parsed records: " + records.size() + " for file " + uf.getId());
        return result;
    }

    private List<WorkRecord> parseExcel(MultipartFile file) throws IOException {
        List<WorkRecord> result = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 1) return result;

            // Fixed column indexes: F=5, E=4, J=9 ... AC=28, AQ=42
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String notification = getString(row, 42);   // AQ
                String prodName = getString(row, 5);        // F
                String drawing = getString(row, 4);         // E
                Integer qty = getInt(row, 1);               // B -> 产量

                for (int c = 9; c <= 28; c += 2) {          // J..AC pairs
                    String process = getString(row, c);
                    Double hours = getDouble(row, c + 1);
                    if ((process == null || process.trim().isEmpty()) && hours == null) continue;

                    WorkRecord wr = new WorkRecord();
                    wr.setNotificationNumber(notification);
                    wr.setProductName(prodName);
                    wr.setDrawingNumber(drawing);
                    wr.setPartName(prodName);
                    wr.setPlanQty(qty);
                    wr.setProcessName(process);

                    String code = processService.getCode(process);
                    boolean codeMissing = false;
                    if (code == null || code.trim().isEmpty()) {
                        code = process; // fallback to name
                        codeMissing = true;
                    }
                    wr.setProcessCode(code);
                    wr.setCodeMissing(codeMissing);

                    if (drawing != null && notification != null && code != null) {
                        String bar = drawing + "-" + notification + "-" + code;
                        String clean = sanitizeBarcode(bar);
                        wr.setBarcode(clean);
                        wr.setBarcodeImage(generateBarcode(clean));
                    }

                    wr.setHours(hours);
                    wr.setHoursMissing(hours == null);
                    result.add(wr);
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

    private final DataFormatter formatter = new DataFormatter();

    private Double getDouble(Row row, Integer idx) {
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        String value = formatter.formatCellValue(c);
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return new BigDecimal(value.trim()).doubleValue();
        } catch (NumberFormatException e) {
            return null;
        }
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
                if (c.trim().isEmpty()) continue;
                Worker w = workerService.getByCode(c.trim());
                if (w != null) names.add(w.getName());
            }
            record.setWorkerNames(String.join(",", names));
        }
        if (record.getQualifiedQty() != null && record.getHours() != null) {
            record.setHourSubtotal(record.getQualifiedQty() * record.getHours());
        }
    }

    private void validate(WorkRecord record) {
        if (record.getProcessCode() == null || record.getProcessCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工序代码不能为空");
        }
        if (record.getBarcode() == null || record.getBarcode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "条形码不能为空");
        }
    }

    private byte[] generateBarcode(String text) {
        try {
            Code128Writer writer = new Code128Writer();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.CODE_128, 300, 80);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "png", out);
            return out.toByteArray();
        } catch (WriterException | IOException | IllegalArgumentException e) {
            return null;
        }
    }

    private String sanitizeBarcode(String text) {
        if (text == null) return null;
        return text.replaceAll("[^\\x00-\\x7F]", "");
    }
}
