import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Properties;
import javax.net.ssl.*;

public class TestMimeDirect {
    public static void main(String[] args) throws Exception {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String attachmentFileName = "Today_OCR_Report_" + dateStr + ".xlsx";

        // 1. Generate real Excel workbook (.xlsx)
        byte[] excelBytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Summary");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("User / Uploader");
            header.createCell(1).setCellValue("Cards Uploaded");
            header.createCell(2).setCellValue("Share");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Jyoti Sonani");
            row1.createCell(1).setCellValue(12);
            row1.createCell(2).setCellValue("60.0%");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Ashutosh Vishwakarma");
            row2.createCell(1).setCellValue(8);
            row2.createCell(2).setCellValue("40.0%");

            Row totalRow = sheet.createRow(3);
            totalRow.createCell(0).setCellValue("Total");
            totalRow.createCell(1).setCellValue(20);
            totalRow.createCell(2).setCellValue("100.0%");

            workbook.write(out);
            excelBytes = out.toByteArray();
        }

        // 2. Build full HTML dashboard email matching GFF template
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
                .user-table { width: 100%; border-collapse: collapse; margin: 16px 0; border: 1px solid #e2e8f0; border-radius: 6px; overflow: hidden; }
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
                      <tr>
                        <td style="padding: 10px 14px; border-bottom: 1px solid #e2e8f0; font-weight: 500; color: #1e293b;">Jyoti Sonani</td>
                        <td style="padding: 10px 14px; border-bottom: 1px solid #e2e8f0; text-align: right; font-weight: bold; color: #2563eb;">12</td>
                        <td style="padding: 10px 14px; border-bottom: 1px solid #e2e8f0; text-align: right; color: #64748b;">60.0%%</td>
                      </tr>
                      <tr>
                        <td style="padding: 10px 14px; border-bottom: 1px solid #e2e8f0; font-weight: 500; color: #1e293b;">Ashutosh Vishwakarma</td>
                        <td style="padding: 10px 14px; border-bottom: 1px solid #e2e8f0; text-align: right; font-weight: bold; color: #2563eb;">8</td>
                        <td style="padding: 10px 14px; border-bottom: 1px solid #e2e8f0; text-align: right; color: #64748b;">40.0%%</td>
                      </tr>
                    </tbody>
                    <tfoot>
                      <tr>
                        <td style="text-align: left;">Total</td>
                        <td style="text-align: right; color: #2563eb;">20</td>
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
            """.formatted(dateStr, attachmentFileName);

        // 3. Assemble MimeMessage with HTML body + Excel Attachment
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("alert@qualtechedge.com"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("jyoti.sonani@qualtechedge.com,ashutosh.vishwakarma@qualtechedge.com"));
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
        Socket socket = new Socket("smtp.bizmail.yahoo.com", 587);
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
        System.out.println("AUTH: " + sslReader.readLine());

        sslWriter.write("MAIL FROM:<alert@qualtechedge.com>\r\n"); sslWriter.flush(); sslReader.readLine();

        String[] allRecipients = {
            "jyoti.sonani@qualtechedge.com", "ashutosh.vishwakarma@qualtechedge.com"
        };
        for (String rcpt : allRecipients) {
            sslWriter.write("RCPT TO:<" + rcpt + ">\r\n"); sslWriter.flush(); sslReader.readLine();
        }

        sslWriter.write("DATA\r\n"); sslWriter.flush(); sslReader.readLine();

        msg.writeTo(ssl.getOutputStream());
        ssl.getOutputStream().write("\r\n.\r\n".getBytes());
        ssl.getOutputStream().flush();

        System.out.println("DELIVERY RESULT: " + sslReader.readLine());
        sslWriter.write("QUIT\r\n"); sslWriter.flush();
        socket.close();
    }
}
