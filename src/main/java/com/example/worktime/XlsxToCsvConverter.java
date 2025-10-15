package com.example.worktime;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.DateUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Command line utility that converts a worksheet from an XLSX workbook to CSV output.
 */
public final class XlsxToCsvConverter {

    static {
        System.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "OFF");
    }

    private XlsxToCsvConverter() {
        // Utility class
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: java XlsxToCsvConverter <input.xlsx> <output.csv> [sheetName]");
            System.exit(1);
        }

        File input = new File(args[0]);
        File output = new File(args[1]);
        String sheetName = args.length == 3 ? args[2] : null;

        if (!input.isFile()) {
            throw new IOException("Input file not found: " + input.getAbsolutePath());
        }

        try (FileInputStream in = new FileInputStream(input);
             Workbook workbook = WorkbookFactory.create(in)) {

            Sheet sheet = selectSheet(workbook, sheetName);
            writeSheetAsCsv(sheet, output);
        }
    }

    private static Sheet selectSheet(Workbook workbook, String sheetName) throws IOException {
        if (sheetName != null && !sheetName.isEmpty()) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IOException("Sheet not found: " + sheetName);
            }
            return sheet;
        }
        return workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0)
                : null;
    }

    private static void writeSheetAsCsv(Sheet sheet, File output) throws IOException {
        if (sheet == null) {
            throw new IOException("Workbook does not contain any sheets");
        }

        DataFormatter formatter = new DataFormatter(Locale.CHINA, true);
        FormulaEvaluator evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
        int maxColumns = determineMaxColumns(sheet);

        boolean[] dateColumns = identifyDateColumns(sheet, maxColumns, formatter, evaluator);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8))) {
            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<String> cells = new ArrayList<>(maxColumns);
                for (int columnIndex = 0; columnIndex < maxColumns; columnIndex++) {
                    boolean treatAsDate = columnIndex < dateColumns.length && dateColumns[columnIndex];
                    cells.add(formatCellValue(row, columnIndex, treatAsDate, formatter, evaluator));
                }
                writer.write(String.join(",", cells));
                writer.newLine();
            }
        }
    }

    private static boolean[] identifyDateColumns(Sheet sheet, int maxColumns, DataFormatter formatter, FormulaEvaluator evaluator) {
        boolean[] dateColumns = new boolean[maxColumns];
        int headerRowIndex = sheet.getFirstRowNum();
        for (int rowIndex = headerRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            boolean hasText = false;
            for (int columnIndex = 0; columnIndex < maxColumns; columnIndex++) {
                Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) {
                    continue;
                }
                String value = formatter.formatCellValue(cell, evaluator).trim();
                if (!value.isEmpty()) {
                    hasText = true;
                }
                if (value.contains("日期") || value.toLowerCase(Locale.ROOT).contains("date")) {
                    dateColumns[columnIndex] = true;
                }
            }
            if (hasText) {
                break;
            }
        }
        return dateColumns;
    }

    private static int determineMaxColumns(Sheet sheet) {
        int maxColumns = 0;
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                int lastCell = row.getLastCellNum();
                if (lastCell > maxColumns) {
                    maxColumns = lastCell;
                }
            }
        }
        return Math.max(0, maxColumns);
    }

    private static String formatCellValue(Row row, int columnIndex, boolean treatAsDate,
            DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = evaluator.evaluateFormulaCell(cell);
        }

        if (cellType == CellType.NUMERIC) {
            double numericValue = cell.getNumericCellValue();
            if (isLikelyExcelDate(cell, numericValue, treatAsDate)) {
                LocalDateTime dateTime = DateUtil.getLocalDateTime(numericValue);
                if (dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)) {
                    return escapeForCsv(dateTime.toLocalDate().toString());
                }
                return escapeForCsv(dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        }

        String formatted = formatter.formatCellValue(cell, evaluator);

        formatted = normalizeWhitespace(formatted);
        return escapeForCsv(formatted);
    }

    private static boolean isLikelyExcelDate(Cell cell, double numericValue, boolean treatAsDate) {
        if (!DateUtil.isValidExcelDate(numericValue)) {
            return false;
        }
        if (DateUtil.isCellDateFormatted(cell)) {
            return true;
        }
        String format = cell.getCellStyle().getDataFormatString();
        if (format != null) {
            String normalized = format.toLowerCase(Locale.ROOT);
            if (normalized.contains("yy") || normalized.contains("mm") || normalized.contains("dd")) {
                return true;
            }
        }
        return treatAsDate;
    }

    private static String normalizeWhitespace(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        normalized = normalized.replace("\r\n", "\n");
        normalized = normalized.replace('\r', '\n');
        return normalized;
    }

    private static String escapeForCsv(String value) {
        if (value.isEmpty()) {
            return value;
        }
        boolean needsQuotes = value.contains(",") || value.contains("\n")
                || value.contains("\r") || value.contains("\"");
        String escaped = value.replace("\"", "\"\"");
        if (needsQuotes) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
