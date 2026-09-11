package com.gff.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GenerateExcelReportsDirectly {

    private static final String DB_URL = "jdbc:postgresql://18.60.179.46:5432/visiting_card_db";
    private static final String DB_USER = "visiting_card";
    private static final String DB_PASS = "qualtechedge";

    public static void main(String[] args) {
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        LocalDateTime now = LocalDateTime.now();
        String timeStamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String dateStamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        System.out.println("=================================================================");
        System.out.println("  GFF Excel Report Generator (Timestamp: " + timeStamp + ")");
        System.out.println("=================================================================");

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found!");
            return;
        }

        Properties props = new Properties();
        props.setProperty("user", DB_USER);
        props.setProperty("password", DB_PASS);
        props.setProperty("options", "-c timezone=UTC");

        try (Connection conn = DriverManager.getConnection(DB_URL, props)) {
            System.out.println("[1/3] Connected to database successfully.");

            new File("reports").mkdirs();

            // 1. Export Consolidated Report
            System.out.println("[2/3] Generating Consolidated Report (All COMPLETED records)...");
            generateConsolidated(conn, timeStamp, dateStamp);

            // 2. Export Today's Report
            System.out.println("[3/3] Generating Today's Report...");
            generateToday(conn, timeStamp, dateStamp);

            System.out.println("=================================================================");
            System.out.println("  SUCCESS! New reports generated with timestamp: " + timeStamp);
            System.out.println("=================================================================");
        } catch (Exception e) {
            System.err.println("Error generating reports: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void generateConsolidated(Connection conn, String timeStamp, String dateStamp) throws Exception {
        String sql = "SELECT * FROM visiting_cards WHERE ocr_status = 'COMPLETED' ORDER BY id DESC";
        List<Map<String, Object>> rows = fetchRows(conn, sql);
        System.out.println("  -> Found " + rows.size() + " completed records.");

        String title = "Consolidated OCR Report (Completed)";
        String tsFileName = "Consolidated_OCR_Report_" + timeStamp + ".xlsx";
        String dateFileName = "Consolidated_OCR_Report_" + dateStamp + ".xlsx";

        // Save timestamped versions
        writeWorkbook(rows, title, "reports/" + tsFileName);
        writeWorkbook(rows, title, "reports/" + dateFileName);
        writeWorkbook(rows, title, "reports/Consolidated_OCR_Report.xlsx");
        writeWorkbook(rows, title, tsFileName);
        writeWorkbook(rows, title, dateFileName);
        writeWorkbook(rows, title, "Consolidated_OCR_Report.xlsx");

        System.out.println("  -> Saved: reports/" + tsFileName);
    }

    private static void generateToday(Connection conn, String timeStamp, String dateStamp) throws Exception {
        String sql = "SELECT * FROM visiting_cards WHERE created_at >= CURRENT_DATE AND created_at < CURRENT_DATE + INTERVAL '1 day' ORDER BY id DESC";
        List<Map<String, Object>> rows = fetchRows(conn, sql);
        if (rows.isEmpty()) {
            System.out.println("  -> No records strictly created today. Fetching unemailed completed records as fallback...");
            String fallbackSql = "SELECT * FROM visiting_cards WHERE ocr_status = 'COMPLETED' AND email_sent_at IS NULL ORDER BY id DESC";
            rows = fetchRows(conn, fallbackSql);
            if (rows.isEmpty()) {
                System.out.println("  -> Exporting all completed records...");
                String demoSql = "SELECT * FROM visiting_cards WHERE ocr_status = 'COMPLETED' ORDER BY id DESC";
                rows = fetchRows(conn, demoSql);
            }
        }
        System.out.println("  -> Found " + rows.size() + " records for Today's report.");

        String title = "Today's OCR & Upload Summary";
        String tsFileName = "Today_OCR_Report_" + timeStamp + ".xlsx";
        String dateFileName = "Today_OCR_Report_" + dateStamp + ".xlsx";

        // Save timestamped versions
        writeWorkbook(rows, title, "reports/" + tsFileName);
        writeWorkbook(rows, title, "reports/" + dateFileName);
        writeWorkbook(rows, title, "reports/Today_OCR_Report.xlsx");
        writeWorkbook(rows, title, tsFileName);
        writeWorkbook(rows, title, dateFileName);
        writeWorkbook(rows, title, "Today_OCR_Report.xlsx");

        System.out.println("  -> Saved: reports/" + tsFileName);
    }

    private static List<Map<String, Object>> fetchRows(Connection conn, String sql) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    map.put(meta.getColumnName(i).toLowerCase(), rs.getObject(i));
                }
                list.add(map);
            }
        }
        return list;
    }

    private static void writeWorkbook(List<Map<String, Object>> rows, String title, String filename) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Reusable styles
            CellStyle headerStyle = wb.createCellStyle();
            Font hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle titleStyle = wb.createCellStyle();
            Font tFont = wb.createFont();
            tFont.setBold(true);
            tFont.setFontHeightInPoints((short) 14);
            tFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(tFont);

            CellStyle dataStyle = wb.createCellStyle();
            Font dFont = wb.createFont();
            dFont.setFontHeightInPoints((short) 9);
            dataStyle.setFont(dFont);

            // User breakdown
            Map<String, Long> userCounts = new LinkedHashMap<>();
            for (Map<String, Object> r : rows) {
                String uploader = (String) r.get("uploader_name");
                if (uploader == null || uploader.isBlank()) {
                    uploader = (String) r.get("uploader_email");
                }
                if (uploader == null || uploader.isBlank()) {
                    uploader = "Unknown User";
                }
                userCounts.put(uploader, userCounts.getOrDefault(uploader, 0L) + 1);
            }

            // Sheet 1: Summary
            Sheet summarySheet = wb.createSheet("Summary");
            summarySheet.setDisplayGridlines(true);
            int rIdx = 0;

            Row tRow = summarySheet.createRow(rIdx++);
            Cell tCell = tRow.createCell(0);
            tCell.setCellValue("Global Fintech Fest (GFF) - " + title);
            tCell.setCellStyle(titleStyle);

            Row dRow = summarySheet.createRow(rIdx++);
            dRow.createCell(0).setCellValue("Report Generated At: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            rIdx++;

            Row ubHeader = summarySheet.createRow(rIdx++);
            Cell u1 = ubHeader.createCell(0); u1.setCellValue("User / Uploader"); u1.setCellStyle(headerStyle);
            Cell u2 = ubHeader.createCell(1); u2.setCellValue("Cards Uploaded"); u2.setCellStyle(headerStyle);
            Cell u3 = ubHeader.createCell(2); u3.setCellValue("Share of Total"); u3.setCellStyle(headerStyle);

            long total = rows.size();
            for (Map.Entry<String, Long> e : userCounts.entrySet()) {
                Row row = summarySheet.createRow(rIdx++);
                Cell c0 = row.createCell(0); c0.setCellValue(e.getKey()); c0.setCellStyle(dataStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(e.getValue()); c1.setCellStyle(dataStyle);
                Cell c2 = row.createCell(2);
                c2.setCellValue(total > 0 ? String.format("%.1f%%", ((double) e.getValue() / total) * 100.0) : "0.0%");
                c2.setCellStyle(dataStyle);
            }

            Row totalRow = summarySheet.createRow(rIdx++);
            Cell tot1 = totalRow.createCell(0); tot1.setCellValue("Total"); tot1.setCellStyle(headerStyle);
            Cell tot2 = totalRow.createCell(1); tot2.setCellValue(total); tot2.setCellStyle(headerStyle);
            Cell tot3 = totalRow.createCell(2); tot3.setCellValue("100.0%"); tot3.setCellStyle(headerStyle);

            for (int i = 0; i < 3; i++) summarySheet.autoSizeColumn(i);

            // Sheet 2: All Documents
            Sheet docSheet = wb.createSheet("All Documents");
            docSheet.setDisplayGridlines(true);

            String[] columns = {
                    "Document ID", "Name", "Job Title", "Company Name", "Department",
                    "Email Address", "Mobile Number", "Work Number", "Website URL",
                    "City", "State", "Postal/ZIP Code", "Country", "LinkedIn", "Twitter/X",
                    "Address", "Status", "Uploaded By", "S3 File URL", "Uploaded At"
            };

            Row headerRow = docSheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Map<String, Object> r : rows) {
                Row row = docSheet.createRow(rowNum++);
                int col = 0;
                setCell(row, col++, r.get("record_id"), dataStyle);
                setCell(row, col++, r.get("card_holder_name"), dataStyle);
                setCell(row, col++, r.get("designation"), dataStyle);
                setCell(row, col++, r.get("company_name"), dataStyle);
                setCell(row, col++, r.get("department"), dataStyle);
                setCell(row, col++, r.get("extracted_email"), dataStyle);
                setCell(row, col++, r.get("extracted_mobile"), dataStyle);
                setCell(row, col++, r.get("work_number"), dataStyle);
                setCell(row, col++, r.get("website_url"), dataStyle);
                setCell(row, col++, r.get("city"), dataStyle);
                setCell(row, col++, r.get("state"), dataStyle);
                setCell(row, col++, r.get("postal_zip_code"), dataStyle);
                setCell(row, col++, r.get("country"), dataStyle);
                setCell(row, col++, r.get("linkedin"), dataStyle);
                setCell(row, col++, r.get("twitter"), dataStyle);
                setCell(row, col++, r.get("extracted_address"), dataStyle);
                setCell(row, col++, r.get("ocr_status"), dataStyle);

                String uploader = (String) r.get("uploader_name");
                String uEmail = (String) r.get("uploader_email");
                String uDisp = (uploader != null && !uploader.isBlank()) ? uploader + " (" + uEmail + ")" : (uEmail != null ? uEmail : "N/A");
                setCell(row, col++, uDisp, dataStyle);

                setCell(row, col++, r.get("image_url"), dataStyle);
                setCell(row, col++, r.get("created_at"), dataStyle);
            }

            for (int i = 0; i < columns.length; i++) {
                try {
                    docSheet.autoSizeColumn(i);
                } catch (Exception ignored) {}
            }

            try (FileOutputStream fos = new FileOutputStream(filename)) {
                wb.write(fos);
            }
        }
    }

    private static void setCell(Row row, int col, Object val, CellStyle style) {
        Cell cell = row.createCell(col);
        if (val == null) {
            cell.setCellValue("N/A");
        } else {
            String str = val.toString();
            if (str.length() > 32000) {
                str = str.substring(0, 32000) + "... [TRUNCATED]";
            }
            cell.setCellValue(str);
        }
        cell.setCellStyle(style);
    }
}
