package com.gff.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;

/**
 * Service to construct and dispatch daily OCR report emails
 * with attached Excel spreadsheet using JavaMailSender and MimeMessageHelper.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.lead-email:jyoti.sonani@qualtechedge.com,ashutosh.vishwakarma@qualtechedge.com}")
    private String leadEmailsConfig;

    @Value("${app.mail.from-email:noreply-gff@qualtechedge.com}")
    private String fromEmail;

    /**
     * Dispatches daily OCR report email with .xlsx attachment to configured team lead recipients.
     *
     * @param excelBytes   Byte array of generated Excel workbook
     * @param reportDate   Date of report
     * @param stats        Summary statistics (total, completed, failed, successRate)
     * @return Result status description
     */
    public String sendDailyOcrReport(byte[] excelBytes, LocalDate reportDate, Map<String, Object> stats) {
        String dateStr = reportDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String attachmentFileName = "Daily_OCR_Report_" + dateStr + ".xlsx";

        // Always save a persistent copy to disk for safety and inspection
        saveReportBackupToDisk(excelBytes, attachmentFileName);

        String[] recipients = Arrays.stream(leadEmailsConfig.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .toArray(String[]::new);

        if (recipients.length == 0) {
            log.warn("No recipient emails configured in app.mail.lead-email.");
            return "SKIPPED: No recipient emails configured";
        }

        if (mailSender == null) {
            log.warn("JavaMailSender bean not available. Report saved locally to reports/{}", attachmentFileName);
            return "SAVED_LOCAL: JavaMailSender unavailable, saved to reports/" + attachmentFileName;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipients);
            helper.setSubject("[Daily Report] OCR Processing & Upload Summary - " + dateStr);

            String htmlBody = buildHtmlEmailBody(dateStr, stats);
            helper.setText(htmlBody, true);

            ByteArrayResource attachmentResource = new ByteArrayResource(excelBytes);
            helper.addAttachment(
                    attachmentFileName,
                    attachmentResource,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );

            log.info("Sending Daily OCR Report email to: {} with attachment {}", Arrays.toString(recipients), attachmentFileName);
            mailSender.send(message);
            log.info("Daily OCR Report email successfully delivered to {}", Arrays.toString(recipients));

            return "SUCCESS: Email delivered to " + String.join(", ", recipients);

        } catch (MessagingException e) {
            log.error("Failed to construct MimeMessage for daily OCR report: {}", e.getMessage(), e);
            return "ERROR: MessagingException - " + e.getMessage();
        } catch (Exception e) {
            log.error("Failed to send daily OCR report email: {}", e.getMessage(), e);
            return "FAILED_TO_SEND: " + e.getMessage() + " (Report safely stored locally in reports/" + attachmentFileName + ")";
        }
    }

    private String buildHtmlEmailBody(String dateStr, Map<String, Object> stats) {
        long total = stats.get("total") != null ? ((Number) stats.get("total")).longValue() : 0;
        long completed = stats.get("completed") != null ? ((Number) stats.get("completed")).longValue() : 0;
        long failed = stats.get("failed") != null ? ((Number) stats.get("failed")).longValue() : 0;
        double successRate = stats.get("successRate") != null ? ((Number) stats.get("successRate")).doubleValue() : 0.0;

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f7f6; margin: 0; padding: 20px; }
                    .card { background-color: #ffffff; border-radius: 8px; padding: 25px; max-width: 650px; margin: 0 auto; box-shadow: 0 4px 6px rgba(0,0,0,0.08); border-top: 5px solid #1E40AF; }
                    .header { text-align: left; border-bottom: 2px solid #E5E7EB; padding-bottom: 15px; margin-bottom: 20px; }
                    .title { font-size: 20px; font-weight: 700; color: #1F2937; margin: 0; }
                    .subtitle { font-size: 13px; color: #6B7280; margin-top: 4px; }
                    .stats-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .stats-table th { background-color: #F3F4F6; color: #374151; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; padding: 10px 14px; text-align: left; border: 1px solid #E5E7EB; }
                    .stats-table td { padding: 12px 14px; font-size: 14px; border: 1px solid #E5E7EB; color: #1F2937; }
                    .badge-success { color: #047857; font-weight: bold; }
                    .badge-failed { color: #B91C1C; font-weight: bold; }
                    .footer { font-size: 12px; color: #9CA3AF; margin-top: 25px; border-top: 1px solid #E5E7EB; padding-top: 15px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <div class="title">Global Fintech Fest (GFF) - Daily OCR Summary</div>
                        <div class="subtitle">Report Date: <strong>%s</strong> | Automated Cron Workflow</div>
                    </div>
                    <p style="font-size: 14px; color: #374151; line-height: 1.5;">
                        Hello Team Lead,<br><br>
                        The automated daily OCR processing and audit workflow for <strong>%s</strong> has completed. Below is the summary of documents uploaded and processed:
                    </p>
                    <table class="stats-table">
                        <thead>
                            <tr>
                                <th>Metric</th>
                                <th>Count / Value</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><strong>Total Documents Uploaded Today</strong></td>
                                <td><strong>%d</strong></td>
                            </tr>
                            <tr>
                                <td><strong>Successfully Processed (OCR Completed)</strong></td>
                                <td><span class="badge-success">%d</span></td>
                            </tr>
                            <tr>
                                <td><strong>Failed / Requires Review</strong></td>
                                <td><span class="badge-failed">%d</span></td>
                            </tr>
                            <tr>
                                <td><strong>Success Rate</strong></td>
                                <td><strong>%.1f%%</strong></td>
                            </tr>
                        </tbody>
                    </table>
                    <p style="font-size: 13px; color: #4B5563;">
                        &#128206; <strong>Attachment:</strong> Please find the detailed Excel spreadsheet attached containing the complete audit log, cardholder details, S3 links, and extracted OCR text.
                    </p>
                    <div class="footer">
                        This is an automated report generated by the GFF Backend Scheduler Service.<br>
                        Confidential &copy; 2026 Global Fintech Fest (GFF). All rights reserved.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(dateStr, dateStr, total, completed, failed, successRate);
    }

    private void saveReportBackupToDisk(byte[] excelBytes, String fileName) {
        try {
            Path dir = Paths.get("reports");
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path targetFile = dir.resolve(fileName);
            Files.write(targetFile, excelBytes);
            log.info("Backup of daily report successfully saved to disk: {}", targetFile.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Could not save local disk backup of report: {}", e.getMessage());
        }
    }
}
