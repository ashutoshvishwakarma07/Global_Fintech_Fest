import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.net.ssl.*;

public class TestMimeDirect {

    static class CardRecord {
        String recordId;
        String name;
        String jobTitle;
        String companyName;
        String department;
        String email;
        String mobile;
        String workNumber;
        String website;
        String city;
        String state;
        String postalCode;
        String country;
        String linkedin;
        String twitter;
        String ocrStatus;
        String uploaderName;
        String uploaderEmail;
        String s3Url;
        String createdAt;
    }

    public static void main(String[] args) throws Exception {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String attachmentFileName = "Today_OCR_Report_" + dateStr + ".xlsx";

        // 1. Fetch live records from PostgreSQL database
        List<CardRecord> records = new ArrayList<>();
        Map<String, Long> userWiseCounts = new LinkedHashMap<>();

        String dbUrl = "jdbc:postgresql://18.60.179.46:5432/visiting_card_db";
        String dbUser = "visiting_card";
        String dbPass = "qualtechedge";

        System.out.println("Querying PostgreSQL database: " + dbUrl + " ...");
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                String sql = "SELECT record_id, card_holder_name, designation, company_name, department, " +
                        "extracted_email, extracted_mobile, work_number, website_url, city, state, postal_zip_code, " +
                        "country, linkedin, twitter, ocr_status, uploader_name, uploader_email, image_url, created_at " +
                        "FROM visiting_cards WHERE DATE(created_at) = CURRENT_DATE ORDER BY id DESC";

                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        CardRecord r = new CardRecord();
                        r.recordId = rs.getString("record_id");
                        r.name = rs.getString("card_holder_name");
                        r.jobTitle = rs.getString("designation");
                        r.companyName = rs.getString("company_name");
                        r.department = rs.getString("department");
                        r.email = rs.getString("extracted_email");
                        r.mobile = rs.getString("extracted_mobile");
                        r.workNumber = rs.getString("work_number");
                        r.website = rs.getString("website_url");
                        r.city = rs.getString("city");
                        r.state = rs.getString("state");
                        r.postalCode = rs.getString("postal_zip_code");
                        r.country = rs.getString("country");
                        r.linkedin = rs.getString("linkedin");
                        r.twitter = rs.getString("twitter");
                        r.ocrStatus = rs.getString("ocr_status");
                        r.uploaderName = rs.getString("uploader_name");
                        r.uploaderEmail = rs.getString("uploader_email");
                        r.s3Url = rs.getString("image_url");
                        Timestamp ts = rs.getTimestamp("created_at");
                        r.createdAt = ts != null ? ts.toString() : "";
                        records.add(r);

                        String uploader = (r.uploaderName != null && !r.uploaderName.isBlank())
                                ? r.uploaderName : (r.uploaderEmail != null && !r.uploaderEmail.isBlank() ? r.uploaderEmail : "Unknown User");
                        userWiseCounts.put(uploader, userWiseCounts.getOrDefault(uploader, 0L) + 1L);
                    }
                }
            }
            System.out.println("Found " + records.size() + " records uploaded today from database.");
        } catch (Exception e) {
            System.err.println("Database query notice: " + e.getMessage() + " (will proceed with empty structure)");
        }

        long totalDocs = records.size();

        // 2. Generate full professional 2-sheet Excel workbook
        byte[] excelBytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Title style
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);

            // Data cell style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // -------------------------------------------------------------
            // SHEET 1: Summary Sheet
            // -------------------------------------------------------------
            Sheet summarySheet = workbook.createSheet("Summary");
            summarySheet.setDisplayGridlines(true);

            int rIdx = 0;
            Row tRow = summarySheet.createRow(rIdx++);
            tRow.createCell(0).setCellValue("Global Fintech Fest (GFF) - Today's OCR & Upload Summary");
            tRow.getCell(0).setCellStyle(titleStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            Row dRow = summarySheet.createRow(rIdx++);
            dRow.createCell(0).setCellValue("Report Date: " + dateStr + " | Total Cards Uploaded: " + totalDocs);
            summarySheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

            rIdx++; // spacing

            // Section: User-wise breakdown
            Row uSec = summarySheet.createRow(rIdx++);
            uSec.createCell(0).setCellValue("User-Wise Upload Breakdown");
            uSec.getCell(0).setCellStyle(titleStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, 2));

            Row uHdr = summarySheet.createRow(rIdx++);
            String[] uCols = {"User / Uploader", "Cards Uploaded", "Share of Total"};
            for (int i = 0; i < uCols.length; i++) {
                Cell c = uHdr.createCell(i);
                c.setCellValue(uCols[i]);
                c.setCellStyle(headerStyle);
            }

            if (userWiseCounts.isEmpty()) {
                Row emptyRow = summarySheet.createRow(rIdx++);
                Cell c = emptyRow.createCell(0);
                c.setCellValue("No uploader data available");
                c.setCellStyle(dataStyle);
                emptyRow.createCell(1).setCellValue(0);
                emptyRow.getCell(1).setCellStyle(dataStyle);
                emptyRow.createCell(2).setCellValue("0.0%");
                emptyRow.getCell(2).setCellStyle(dataStyle);
            } else {
                for (Map.Entry<String, Long> entry : userWiseCounts.entrySet()) {
                    Row row = summarySheet.createRow(rIdx++);
                    Cell c1 = row.createCell(0); c1.setCellValue(entry.getKey()); c1.setCellStyle(dataStyle);
                    Cell c2 = row.createCell(1); c2.setCellValue(entry.getValue()); c2.setCellStyle(dataStyle);
                    Cell c3 = row.createCell(2);
                    double pct = totalDocs > 0 ? ((double) entry.getValue() / totalDocs) * 100.0 : 0.0;
                    c3.setCellValue(String.format("%.1f%%", pct));
                    c3.setCellStyle(dataStyle);
                }
            }

            Row totalRow = summarySheet.createRow(rIdx++);
            Cell tr1 = totalRow.createCell(0); tr1.setCellValue("Total"); tr1.setCellStyle(headerStyle);
            Cell tr2 = totalRow.createCell(1); tr2.setCellValue(totalDocs); tr2.setCellStyle(headerStyle);
            Cell tr3 = totalRow.createCell(2); tr3.setCellValue("100.0%"); tr3.setCellStyle(headerStyle);

            rIdx++; // spacing

            // Section: Extracted Cards Table Header
            Row cSec = summarySheet.createRow(rIdx++);
            cSec.createCell(0).setCellValue("Extracted Visiting Cards - Summary");
            cSec.getCell(0).setCellStyle(titleStyle);

            String[] cardCols = {
                    "#", "Record ID", "Name", "Job Title", "Company Name", "Department",
                    "Email Address", "Mobile Number", "Work Number", "Website URL",
                    "City", "State", "Postal Code", "Country", "LinkedIn", "Twitter/X", "OCR Status", "Uploaded By"
            };
            Row cHdr = summarySheet.createRow(rIdx++);
            for (int i = 0; i < cardCols.length; i++) {
                Cell c = cHdr.createCell(i);
                c.setCellValue(cardCols[i]);
                c.setCellStyle(headerStyle);
            }

            if (records.isEmpty()) {
                Row emptyCardRow = summarySheet.createRow(rIdx++);
                Cell ec = emptyCardRow.createCell(0);
                ec.setCellValue("No visiting cards found for today.");
                ec.setCellStyle(dataStyle);
                summarySheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, cardCols.length - 1));
            } else {
                int cIdx = 1;
                for (CardRecord r : records) {
                    Row row = summarySheet.createRow(rIdx++);
                    row.createCell(0).setCellValue(cIdx++);
                    row.createCell(1).setCellValue(r.recordId != null ? r.recordId : "");
                    row.createCell(2).setCellValue(r.name != null ? r.name : "");
                    row.createCell(3).setCellValue(r.jobTitle != null ? r.jobTitle : "");
                    row.createCell(4).setCellValue(r.companyName != null ? r.companyName : "");
                    row.createCell(5).setCellValue(r.department != null ? r.department : "");
                    row.createCell(6).setCellValue(r.email != null ? r.email : "");
                    row.createCell(7).setCellValue(r.mobile != null ? r.mobile : "");
                    row.createCell(8).setCellValue(r.workNumber != null ? r.workNumber : "");
                    row.createCell(9).setCellValue(r.website != null ? r.website : "");
                    row.createCell(10).setCellValue(r.city != null ? r.city : "");
                    row.createCell(11).setCellValue(r.state != null ? r.state : "");
                    row.createCell(12).setCellValue(r.postalCode != null ? r.postalCode : "");
                    row.createCell(13).setCellValue(r.country != null ? r.country : "");
                    row.createCell(14).setCellValue(r.linkedin != null ? r.linkedin : "");
                    row.createCell(15).setCellValue(r.twitter != null ? r.twitter : "");
                    row.createCell(16).setCellValue(r.ocrStatus != null ? r.ocrStatus : "");
                    row.createCell(17).setCellValue(r.uploaderName != null ? r.uploaderName : (r.uploaderEmail != null ? r.uploaderEmail : ""));
                    for (int j = 0; j < cardCols.length; j++) {
                        if (row.getCell(j) != null) row.getCell(j).setCellStyle(dataStyle);
                    }
                }
            }

            for (int i = 0; i < cardCols.length; i++) {
                summarySheet.autoSizeColumn(i);
            }

            // -------------------------------------------------------------
            // SHEET 2: All Documents Sheet
            // -------------------------------------------------------------
            Sheet allSheet = workbook.createSheet("All Documents");
            allSheet.setDisplayGridlines(true);

            String[] allCols = {
                    "#", "Record ID", "Card Holder Name", "Company Name", "Job Title",
                    "Mobile", "Work Number", "Email", "Website", "City", "State",
                    "Postal Code", "Country", "LinkedIn", "Twitter", "Image S3 URL", "OCR Status", "Uploader Name", "Uploaded At"
            };
            Row allHdr = allSheet.createRow(0);
            for (int i = 0; i < allCols.length; i++) {
                Cell c = allHdr.createCell(i);
                c.setCellValue(allCols[i]);
                c.setCellStyle(headerStyle);
            }

            if (records.isEmpty()) {
                Row emptyDocRow = allSheet.createRow(1);
                Cell edc = emptyDocRow.createCell(0);
                edc.setCellValue("No documents uploaded today.");
                edc.setCellStyle(dataStyle);
                allSheet.addMergedRegion(new CellRangeAddress(1, 1, 0, allCols.length - 1));
            } else {
                int aIdx = 1;
                for (CardRecord r : records) {
                    Row row = allSheet.createRow(aIdx);
                    row.createCell(0).setCellValue(aIdx++);
                    row.createCell(1).setCellValue(r.recordId != null ? r.recordId : "");
                    row.createCell(2).setCellValue(r.name != null ? r.name : "");
                    row.createCell(3).setCellValue(r.companyName != null ? r.companyName : "");
                    row.createCell(4).setCellValue(r.jobTitle != null ? r.jobTitle : "");
                    row.createCell(5).setCellValue(r.mobile != null ? r.mobile : "");
                    row.createCell(6).setCellValue(r.workNumber != null ? r.workNumber : "");
                    row.createCell(7).setCellValue(r.email != null ? r.email : "");
                    row.createCell(8).setCellValue(r.website != null ? r.website : "");
                    row.createCell(9).setCellValue(r.city != null ? r.city : "");
                    row.createCell(10).setCellValue(r.state != null ? r.state : "");
                    row.createCell(11).setCellValue(r.postalCode != null ? r.postalCode : "");
                    row.createCell(12).setCellValue(r.country != null ? r.country : "");
                    row.createCell(13).setCellValue(r.linkedin != null ? r.linkedin : "");
                    row.createCell(14).setCellValue(r.twitter != null ? r.twitter : "");
                    row.createCell(15).setCellValue(r.s3Url != null ? r.s3Url : "");
                    row.createCell(16).setCellValue(r.ocrStatus != null ? r.ocrStatus : "");
                    row.createCell(17).setCellValue(r.uploaderName != null ? r.uploaderName : (r.uploaderEmail != null ? r.uploaderEmail : ""));
                    row.createCell(18).setCellValue(r.createdAt != null ? r.createdAt : "");
                    for (int j = 0; j < allCols.length; j++) {
                        if (row.getCell(j) != null) row.getCell(j).setCellStyle(dataStyle);
                    }
                }
            }

            for (int i = 0; i < allCols.length; i++) {
                allSheet.autoSizeColumn(i);
            }

            workbook.write(out);
            excelBytes = out.toByteArray();
        }

        // 3. Build User rows HTML dynamically based on real data
        StringBuilder userRowsHtml = new StringBuilder();
        if (userWiseCounts.isEmpty()) {
            userRowsHtml.append("""
                <tr>
                  <td colspan="3" style="padding: 12px 14px; text-align: center; color: #94a3b8;">No uploader data available</td>
                </tr>
                """);
        } else {
            for (Map.Entry<String, Long> entry : userWiseCounts.entrySet()) {
                String uploader = entry.getKey();
                Long count = entry.getValue();
                double pct = totalDocs > 0 ? ((double) count / totalDocs) * 100.0 : 0.0;
                userRowsHtml.append("""
                    <tr>
                      <td style="padding: 10px 14px; border-bottom: 1px solid #e2e8f0; font-weight: 500; color: #1e293b;">%s</td>
                      <td style="padding: 10px 14px; border-bottom: 1px solid #e2e8f0; text-align: right; font-weight: bold; color: #2563eb;">%d</td>
                      <td style="padding: 10px 14px; border-bottom: 1px solid #e2e8f0; text-align: right; color: #64748b;">%.1f%%</td>
                    </tr>
                    """.formatted(uploader, count, pct));
            }
        }

        // 4. Construct the HTML email body matching the GFF template
        String htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }
                .header { background: #1e3a8a; color: #ffffff; padding: 20px; }
                .header h2 { margin: 0; font-size: 18px; }
                .header p { margin: 4px 0 0 0; font-size: 12px; opacity: 0.8; }
                .content { padding: 20px; }
                .user-table { width: 100%%; border-collapse: collapse; margin: 16px 0; border: 1px solid #e2e8f0; border-radius: 6px; overflow: hidden; }
                .user-table th { background: #f1f5f9; padding: 10px 14px; font-size: 12px; font-weight: bold; color: #475569; text-transform: uppercase; border-bottom: 1px solid #e2e8f0; }
                .user-table tfoot td { background: #f8fafc; padding: 10px 14px; font-size: 13px; font-weight: bold; color: #0f172a; border-top: 2px solid #cbd5e1; }
                .cta-box { background: #eff6ff; border-left: 4px solid #2563eb; padding: 12px 16px; border-radius: 4px; margin-top: 15px; }
                .footer { text-align: center; padding: 15px; font-size: 11px; color: #94a3b8; border-top: 1px solid #f1f5f9; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2>Global Fintech Fest (GFF) — Today's Report</h2>
                  <p>Date: %s | Report Generated Automatically</p>
                </div>
                <div class="content">
                  <p style="color: #334155; font-size: 14px; margin: 0 0 10px 0;">Hi,</p>
                  <p style="color: #64748b; font-size: 13px; line-height: 1.5; margin: 0 0 15px 0;">
                    Please find below the visiting card OCR upload and processing summary:
                  </p>

                  <table class="user-table">
                    <thead>
                      <tr>
                        <th style="text-align: left;">User / Uploader</th>
                        <th style="text-align: right;">Cards Uploaded</th>
                        <th style="text-align: right;">Share</th>
                      </tr>
                    </thead>
                    <tbody>
                      %s
                    </tbody>
                    <tfoot>
                      <tr>
                        <td style="text-align: left;">Total</td>
                        <td style="text-align: right; color: #2563eb;">%d</td>
                        <td style="text-align: right;">100.0%%</td>
                      </tr>
                    </tfoot>
                  </table>

                  <div class="cta-box">
                    <p style="margin: 0; font-size: 13px; font-weight: bold; color: #1e40af;">📎 Detailed Report Attached</p>
                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #3b82f6;">
                      All extracted contact fields (Name, Company, Phone, Email, S3 URLs, Uploaded By) are compiled in the attached <strong>%s</strong> spreadsheet.
                    </p>
                  </div>
                </div>
                <div class="footer">
                  Generated automatically by GFF Backend Service.<br/>
                  Confidential &copy; 2026 Global Fintech Fest.
                </div>
              </div>
            </body>
            </html>
            """.formatted(dateStr, userRowsHtml.toString(), totalDocs, attachmentFileName);

        // 5. Send via direct STARTTLS SMTP to configured test recipients
        // Production Recipients:
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("aksh.sinha@qualtechedge.com,manish.kankani@qualtechedge.com,ashish.srivastava@qualtechedge.com"));
        msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse("naveen.kumar1@qualtechedge.com,amit.sethia@qualtechedge.com"));
        // Test Recipients:
        // msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("jyoti.sonani@qualtechedge.com,ashutosh.vishwakarma@qualtechedge.com"));
        msg.setSubject("[Today's Report] OCR Processing & Upload Summary - " + dateStr);

        MimeMultipart multipart = new MimeMultipart();

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);

        MimeBodyPart attachPart = new MimeBodyPart();
        attachPart.setContent(excelBytes, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        attachPart.setFileName(attachmentFileName);
        multipart.addBodyPart(attachPart);

        msg.setContent(multipart);
        msg.saveChanges();

        System.out.println("Connecting to smtp.bizmail.yahoo.com:587...");
        try (Socket socket = new Socket("smtp.bizmail.yahoo.com", 587)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            reader.readLine();
            writer.write("EHLO localhost\r\n"); writer.flush();
            String line;
            while ((line = reader.readLine()) != null) { if (line.startsWith("250 ")) break; }

            writer.write("STARTTLS\r\n"); writer.flush();
            reader.readLine();

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                }
            }, null);
            SSLSocket ssl = (SSLSocket) sc.getSocketFactory().createSocket(socket, "smtp.bizmail.yahoo.com", 587, true);
            ssl.startHandshake();

            BufferedReader sslReader = new BufferedReader(new InputStreamReader(ssl.getInputStream()));
            BufferedWriter sslWriter = new BufferedWriter(new OutputStreamWriter(ssl.getOutputStream()));

            sslWriter.write("EHLO localhost\r\n"); sslWriter.flush();
            while ((line = sslReader.readLine()) != null) { if (line.startsWith("250 ")) break; }

            sslWriter.write("AUTH LOGIN\r\n"); sslWriter.flush(); sslReader.readLine();
            sslWriter.write(Base64.getEncoder().encodeToString("alert@qualtechedge.com".getBytes()) + "\r\n"); sslWriter.flush(); sslReader.readLine();
            sslWriter.write(Base64.getEncoder().encodeToString("wgcdoupsprrenrjg".getBytes()) + "\r\n"); sslWriter.flush();
            String authResp = sslReader.readLine();
            System.out.println("AUTH: " + authResp);

            sslWriter.write("MAIL FROM:<alert@qualtechedge.com>\r\n"); sslWriter.flush(); sslReader.readLine();

            String[] allRecipients = {
                // "aksh.sinha@qualtechedge.com", "manish.kankani@qualtechedge.com", "ashish.srivastava@qualtechedge.com",
                // "naveen.kumar1@qualtechedge.com", "amit.sethia@qualtechedge.com"
                 "jyoti.sonani@qualtechedge.com", "ashutosh.vishwakarma@qualtechedge.com","naveen.kumar1@qualtechedge.com"
            };
            for (String rcpt : allRecipients) {
                sslWriter.write("RCPT TO:<" + rcpt + ">\r\n"); sslWriter.flush(); sslReader.readLine();
            }

            sslWriter.write("DATA\r\n"); sslWriter.flush(); sslReader.readLine();

            msg.writeTo(ssl.getOutputStream());
            ssl.getOutputStream().write("\r\n.\r\n".getBytes());
            ssl.getOutputStream().flush();

            String sendResp = sslReader.readLine();
            System.out.println("DELIVERY RESULT: " + sendResp);
            sslWriter.write("QUIT\r\n"); sslWriter.flush();
        }
    }
}
