# Worktime Application

This project is a simple work time management demo built with **Spring Boot 2.7.18**, **Vue 2**, and **MySQL**. It allows uploading Excel files, managing workers and process codes, and recording work times. Uploaded Excel files are stored for reference and records with duplicate barcodes are marked as supplemental.

## Requirements

- Java 8+
- Maven 3+
- Node.js 18+
- MySQL 8+

## Backend Setup

1. Create a MySQL database named `worktime` and run [`backend/src/main/resources/schema.sql`](backend/src/main/resources/schema.sql) to create the tables. The file [`backend/src/main/resources/data.sql`](backend/src/main/resources/data.sql) seeds a default admin user.
2. Edit `backend/src/main/resources/application.properties` to configure your MySQL username and password.
3. Build the backend:

```bash
mvn -f backend/pom.xml package
```

4. Run the application:

```bash
java -jar backend/target/worktime-0.0.1-SNAPSHOT.jar
```

## Frontend Setup

1. Install dependencies and build:

```bash
npm install --prefix frontend
npm run --prefix frontend build
```

2. During development you can run `npm run --prefix frontend dev` to start a hot reload server.

The built files will be placed in `frontend/dist` and can be served by any web server.
After登录后,顶部导航栏可在“Excel上传”“扫码录入”“人员管理”和“工序代码”页面之间切换。

## Versions

- Spring Boot **2.7.18**
- Apache POI **5.2.5**
- Vue **2.7.14** with Vite **5.2**

This repository also includes a sample Excel file `新建 Microsoft Excel 工作表.xlsx` for testing uploads.
