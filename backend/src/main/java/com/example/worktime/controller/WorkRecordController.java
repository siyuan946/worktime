package com.example.worktime.controller;

import com.example.worktime.model.WorkRecord;
import com.example.worktime.model.UploadedFile;
import com.example.worktime.repository.WorkRecordRepository;
import com.example.worktime.repository.UploadedFileRepository;
import com.example.worktime.service.OperationLogService;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
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
    private final OperationLogService logService;

    public WorkRecordController(WorkRecordRepository repository, ProcessCodeService processService,
                                WorkerService workerService, UploadedFileRepository fileRepository,
                                OperationLogService logService) {
        this.repository = repository;
        this.processService = processService;
        this.workerService = workerService;
        this.fileRepository = fileRepository;
        this.logService = logService;
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

    @GetMapping("/file/{fileId}/filled")
    public List<WorkRecord> byFileFilled(@PathVariable Long fileId) {
        return repository.findByFileIdAndFilledTrue(fileId);
    }

    @GetMapping("/file/{fileId}/export")
    public void exportFilled(@PathVariable Long fileId, HttpServletResponse response) throws IOException {
        List<WorkRecord> list = repository.findByFileIdAndFilledTrue(fileId);
        String name = fileRepository.findById(fileId).map(UploadedFile::getFileName).orElse("records.xlsx");
        exportList(list, name, response);
    }

    @GetMapping("/date/{date}/export")
    public void exportByDate(@PathVariable @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
                             HttpServletResponse response) throws IOException {
        java.time.LocalDateTime start = date.atStartOfDay();
        java.time.LocalDateTime end = start.plusDays(1);
        List<WorkRecord> list = repository.findByUploadDate(start, end);
        exportList(list, "records_" + date.toString() + ".xlsx", response);
    }

    private java.util.List<String> splitWorkers(String codes) {
        if (codes == null || codes.trim().isEmpty()) return java.util.Collections.emptyList();
        String[] arr = codes.split("[,\u3001\\s]+");
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String s : arr) if (!s.trim().isEmpty()) list.add(s.trim());
        return list;
    }

    private java.util.List<String> splitNames(String names) {
        if (names == null || names.trim().isEmpty()) return java.util.Collections.emptyList();
        String[] arr = names.split("[,\u3001\\s]+");
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String s : arr) if (!s.trim().isEmpty()) list.add(s.trim());
        return list;
    }

    private java.util.List<Double> parseQtys(String str) {
        java.util.List<Double> vals = new java.util.ArrayList<>();
        if (str == null) return vals;
        for (String seg : str.trim().split("[\\s,]+")) {
            if (seg.isEmpty()) continue;
            int idx = seg.indexOf(":");
            if (idx < 0) idx = seg.indexOf('：');
            String num = idx >= 0 ? seg.substring(idx + 1) : seg;
            if (num.isEmpty()) { vals.add(null); continue; }
            try { vals.add(Double.parseDouble(num)); } catch (NumberFormatException e) { vals.add(null); }
        }
        return vals;
    }

    private void exportList(java.util.List<WorkRecord> list, String fileName, HttpServletResponse response) throws IOException {
        Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        Sheet sheet = wb.createSheet("records");
        CellStyle twoDec = wb.createCellStyle();
        twoDec.setDataFormat(wb.createDataFormat().getFormat("0.00"));
        Row head = sheet.createRow(0);
        String[] titles = {"通知单号","产品名称","图号","工序代码","工时","产量","人员代码","姓名","数量分配","工时分配","起始日期","结束日期"};
        for (int i = 0; i < titles.length; i++) {
            head.createCell(i).setCellValue(titles[i]);
        }

        int rowIdx = 1;
        for (WorkRecord r : list) {
            java.util.List<String> codes = splitWorkers(r.getWorkerCodes());
            java.util.List<String> names = splitNames(r.getWorkerNames());
            java.util.List<Double> qtys = parseQtys(r.getWorkerQtys());
            int max = Math.max(1, Math.max(Math.max(codes.size(), names.size()), qtys.size()));

            for (int i = 0; i < max; i++) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(n(r.getNotificationNumber()));
                row.createCell(c++).setCellValue(n(r.getProductName()));
                row.createCell(c++).setCellValue(n(r.getDrawingNumber()));
                row.createCell(c++).setCellValue(n(r.getProcessCode()));
                if (r.getHours() != null) {
                    Cell cell = row.createCell(c++);
                    cell.setCellValue(r.getHours());
                    cell.setCellStyle(twoDec);
                } else row.createCell(c++).setCellValue("");
                if (r.getPlanQty() != null) row.createCell(c++).setCellValue(r.getPlanQty()); else row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue(i < codes.size() ? n(codes.get(i)) : "");
                row.createCell(c++).setCellValue(i < names.size() ? n(names.get(i)) : "");

                Double q = i < qtys.size() ? qtys.get(i) : null;
                if (q != null) {
                    Cell cell = row.createCell(c++);
                    cell.setCellValue(q);
                    cell.setCellStyle(twoDec);
                }
                else row.createCell(c++).setCellValue("");

                Double workerHours = null;
                if (q != null && r.getHours() != null) workerHours = q * r.getHours();
                if (workerHours != null) {
                    Cell cell = row.createCell(c++);
                    cell.setCellValue(workerHours);
                    cell.setCellStyle(twoDec);
                }
                else row.createCell(c++).setCellValue("");

                row.createCell(c++).setCellValue(r.getStartTime() == null ? "" : r.getStartTime().toLocalDate().toString());
                row.createCell(c++).setCellValue(r.getEndTime() == null ? "" : r.getEndTime().toLocalDate().toString());
            }
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fname = fileName == null || fileName.isEmpty() ? "records.xlsx" : java.net.URLEncoder.encode(fileName, "UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + fname);
        wb.write(response.getOutputStream());
        wb.close();
    }

    @GetMapping("/file/{fileId}/print")
    public void printFile(@PathVariable Long fileId, HttpServletResponse response) throws IOException {
        List<WorkRecord> list = repository.findByFileId(fileId);
        ClassPathResource res = new ClassPathResource("template.xlsx");
        try (InputStream in = res.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            int rowIdx = 1;
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            CreationHelper helper = wb.getCreationHelper();
            for (WorkRecord r : list) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) row = sheet.createRow(rowIdx);
                int c = 0;
                row.createCell(c++).setCellValue(rowIdx);
                row.createCell(c++).setCellValue(n(r.getNotificationNumber()));
                row.createCell(c++).setCellValue(n(r.getProductName()));
                row.createCell(c++).setCellValue(n(r.getDrawingNumber()));
                row.createCell(c++).setCellValue(n(r.getProcessCode()));
                if (r.getBarcodeImage() != null) {
                    int picId = wb.addPicture(r.getBarcodeImage(), Workbook.PICTURE_TYPE_PNG);
                    ClientAnchor anchor = helper.createClientAnchor();
                    anchor.setRow1(rowIdx);
                    anchor.setCol1(c);
                    Picture pic = drawing.createPicture(anchor, picId);
                    pic.resize();
                }
                rowIdx++;
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition","attachment; filename=print.xlsx");
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/generateBarcode")
    public String generateBarcodeEndpoint(@RequestParam("text") String text) {
        byte[] img = generateBarcode(sanitizeBarcode(text));
        return img == null ? null : java.util.Base64.getEncoder().encodeToString(img);
    }

    @PutMapping("/{id}")
    @Transactional
    public WorkRecord update(@PathVariable Long id, @RequestBody WorkRecord record,
                             @RequestHeader("X-User") String user) {
        WorkRecord existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "record not found"));
        record.setId(id);
        if (record.getFile() == null) {
            record.setFile(existing.getFile());
        }
        if (record.getBarcode() == null) record.setBarcode(existing.getBarcode());
        if (record.getBarcodeImage() == null) record.setBarcodeImage(existing.getBarcodeImage());
        prepare(record);
        if (record.getQualifiedQty() != null) record.setFilled(true);
        WorkRecord updated = repository.save(record);
        logService.log(user, "更新记录 " + id, null);
        return updated;
    }

    @PostMapping("/duplicate/{id}")
    public WorkRecord duplicate(@PathVariable Long id, @RequestHeader("X-User") String user) {
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
        copy.setFilled(false);
        prepare(copy);
        WorkRecord saved = repository.save(copy);
        logService.log(user, "复制记录 " + id, "newId=" + saved.getId());
        return saved;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id, @RequestHeader("X-User") String user) {
        repository.deleteById(id);
        repository.flush();
        logService.log(user, "删除记录 " + id, null);
    }

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public List<WorkRecord> save(@RequestParam("fileId") Long fileId, @RequestBody List<WorkRecord> records,
                                 @RequestHeader("X-User") String user) {
        UploadedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效文件"));
        for (WorkRecord r : records) {
            r.setFile(file);
            r.setSupplemental(!repository.findByBarcode(r.getBarcode()).isEmpty());
            r.setFilled(false);
            prepare(r);
        }
        java.util.List<WorkRecord> saved = repository.saveAll(records);
        repository.flush();
        System.out.println("Saved records: " + saved.size());
        logService.log(user, "新增记录" , "fileId=" + fileId + " count=" + saved.size());
        return saved;
    }

    @PostMapping("/parse")
    @Transactional
    public Map<String, Object> parse(@RequestParam("file") MultipartFile file,
                                     @RequestHeader("X-User") String user) throws IOException {
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
        logService.log(user, "上传文件 " + uf.getFileName(), "records=" + records.size());
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
                    wr.setFilled(false);
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

    private String n(String v) { return v == null ? "" : v; }
}
