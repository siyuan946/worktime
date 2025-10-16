# Worktime Data Tools

This repository contains a legacy production scheduling worksheet alongside a
Java 8 utility that exports the data into a UTF-8 encoded CSV file so that it
can be processed with version control friendly tooling.

## Contents

- `新建 Microsoft Excel 工作表.xlsx` – original Microsoft Excel workbook.
- `data/worktime.csv` – CSV export of the first worksheet.
- `pom.xml` – Maven configuration for the Java command line utility.
- `src/main/java` – Java sources for the XLSX to CSV exporter.

## Usage

To regenerate the CSV output run:

```bash
mvn -q package
mvn -q -Dexec.mainClass="com.example.worktime.XlsxToCsvConverter" \
  -Dexec.args="'新建 Microsoft Excel 工作表.xlsx' data/worktime.csv" exec:java
```

Pass an optional third argument with the sheet name if you need to export a
specific worksheet instead of the first one. The separate `package` step makes
sure the converter class files are available to the exec plugin, preventing a
`ClassNotFoundException` when the tool is launched from a fresh checkout.

The exporter is built with Java 8, Apache POI, and runs entirely within the
standard Maven toolchain. It performs basic type coercion, including conversion
of Excel date serials into ISO 8601 strings.

## Data

The dataset enumerates production parts, including the manufacturing steps and
estimated work times for each operation. Column headers and values are retained
in Chinese as provided by the source workbook.
