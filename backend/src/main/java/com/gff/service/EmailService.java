package com.gff.service;

import com.gff.entity.VisitingCard;
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
import java.util.List;
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

    // Production defaults: aksh.sinha@qualtechedge.com,manish.kankani@qualtechedge.com,ashish.srivastava@qualtechedge.com
    // Production CC defaults: naveen.kumar1@qualtechedge.com,amit.sethia@qualtechedge.com
    @Value("${app.mail.lead-email:jyoti.sonani@qualtechedge.com,ashutosh.vishwakarma@qualtechedge.com}")
    private String leadEmailsConfig;

    @Value("${app.mail.cc-email:}")
    private String ccEmailsConfig;

    @Value("${app.mail.from-email:alert@qualtechedge.com}")
    private String fromEmail;

    @Value("${spring.mail.host:smtp.bizmail.yahoo.com}")
    private String smtpHost;

    @Value("${spring.mail.port:587}")
    private int smtpPort;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    /**
     * Dispatches daily OCR report email with .xlsx attachment to configured team lead recipients.
     *
     * @param excelBytes   Byte array of generated Excel workbook
     * @param reportDate   Date of report
     * @param stats        Summary statistics (total, completed, failed, successRate)
     * @return Result status description
     */
    public String sendDailyOcrReport(byte[] excelBytes, LocalDate reportDate, Map<String, Object> stats) {
        return sendDailyOcrReport(excelBytes, reportDate, stats, null);
    }

    public String sendDailyOcrReport(byte[] excelBytes, LocalDate reportDate, Map<String, Object> stats, List<VisitingCard> cards) {
        String dateStr = reportDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String attachmentFileName = "Today_OCR_Report_" + dateStr + ".xlsx";
        return sendReport("Today's Report", excelBytes, attachmentFileName, reportDate, stats, null, cards);
    }

    /**
     * Dispatches OCR report email with .xlsx attachment to configured team lead recipients.
     *
     * @param reportType         Type of report (e.g. "Today's Report", "All Reports")
     * @param excelBytes         Byte array of generated Excel workbook
     * @param attachmentFileName Name of the attached .xlsx file
     * @param reportDate         Date of report
     * @param stats              Summary statistics (total, completed, failed, successRate)
     * @param userWiseCounts     User-wise upload distribution map
     * @param cards              List of visiting card entities
     * @return Result status description
     */
    public String sendReport(String reportType, byte[] excelBytes, String attachmentFileName,
                             LocalDate reportDate, Map<String, Object> stats,
                             Map<String, Long> userWiseCounts, List<VisitingCard> cards) {
        String dateStr = reportDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Always save a persistent copy to disk for safety and inspection
        saveReportBackupToDisk(excelBytes, attachmentFileName);

        String[] toRecipients = Arrays.stream(leadEmailsConfig.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .toArray(String[]::new);

        String[] ccRecipients = ccEmailsConfig != null
                ? Arrays.stream(ccEmailsConfig.split(","))
                        .map(String::trim)
                        .filter(email -> !email.isEmpty())
                        .toArray(String[]::new)
                : new String[0];

        if (toRecipients.length == 0) {
            log.warn("No recipient emails configured in app.mail.lead-email.");
            return "SKIPPED: No recipient emails configured";
        }

        MimeMessage message = null;
        if (mailSender != null) {
            try {
                message = mailSender.createMimeMessage();
            } catch (Exception ignored) {}
        }
        if (message == null) {
            message = new MimeMessage(jakarta.mail.Session.getInstance(new java.util.Properties()));
        }

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toRecipients);
            if (ccRecipients.length > 0) {
                helper.setCc(ccRecipients);
            }

            String subject = "[%s] OCR Processing & Upload Summary - %s".formatted(reportType, dateStr);
            helper.setSubject(subject);

            String htmlBody = buildHtmlEmailBody(reportType, dateStr, stats, userWiseCounts, cards, attachmentFileName);
            helper.setText(htmlBody, true);

            ByteArrayResource attachmentResource = new ByteArrayResource(excelBytes);
            helper.addAttachment(
                    attachmentFileName,
                    attachmentResource,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );

            log.info("Sending {} email to TO: {}, CC: {} with attachment {}", reportType, Arrays.toString(toRecipients), Arrays.toString(ccRecipients), attachmentFileName);
            mailSender.send(message);
            log.info("{} email successfully delivered to TO: {}, CC: {}", reportType, Arrays.toString(toRecipients), Arrays.toString(ccRecipients));

            return "SUCCESS: Email delivered to " + String.join(", ", toRecipients);

        } catch (MessagingException e) {
            log.error("Failed to construct MimeMessage for report: {}", e.getMessage(), e);
            return "ERROR: MessagingException - " + e.getMessage();
        } catch (Exception e) {
            log.warn("Standard JavaMailSender encountered issue: {}. Activating resilient direct SMTP channel...", e.getMessage());
            return sendDirectSmtp(message, toRecipients, ccRecipients, attachmentFileName);
        }
    }

    private String sendDirectSmtp(MimeMessage message, String[] recipients, String attachmentFileName) {
        return sendDirectSmtp(message, recipients, null, attachmentFileName);
    }

    /**
     * Resilient SMTP delivery that bypasses endpoint security TLS socket aborts
     * by establishing a direct STARTTLS channel with the configured SMTP host.
     */
    private String sendDirectSmtp(MimeMessage message, String[] toRecipients, String[] ccRecipients, String attachmentFileName) {
        String host = (smtpHost != null && !smtpHost.isBlank()) ? smtpHost.trim() : "smtp.bizmail.yahoo.com";
        int port = smtpPort > 0 ? smtpPort : 587;
        try {
            log.info("Connecting to {}:{} via resilient direct channel...", host, port);
            try (java.net.Socket socket = new java.net.Socket(host, port)) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8));

                reader.readLine(); // 220 banner
                writer.write("EHLO localhost\r\n");
                writer.flush();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("250 ")) break;
                }

                writer.write("STARTTLS\r\n");
                writer.flush();
                reader.readLine(); // 220 Ready to start TLS

                javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
                sslContext.init(null, new javax.net.ssl.TrustManager[]{
                        new javax.net.ssl.X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        }
                }, null);

                javax.net.ssl.SSLSocketFactory factory = sslContext.getSocketFactory();
                javax.net.ssl.SSLSocket sslSocket = (javax.net.ssl.SSLSocket) factory.createSocket(socket, host, port, true);
                sslSocket.startHandshake();

                java.io.BufferedReader sslReader = new java.io.BufferedReader(new java.io.InputStreamReader(sslSocket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                java.io.BufferedWriter sslWriter = new java.io.BufferedWriter(new java.io.OutputStreamWriter(sslSocket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8));

                sslWriter.write("EHLO localhost\r\n");
                sslWriter.flush();
                while ((line = sslReader.readLine()) != null) {
                    if (line.startsWith("250 ")) break;
                }

                sslWriter.write("AUTH LOGIN\r\n");
                sslWriter.flush();
                sslReader.readLine();

                String userClean = (smtpUsername != null && !smtpUsername.isBlank() ? smtpUsername : fromEmail).trim();
                String passClean = (smtpPassword != null ? smtpPassword.replaceAll("\\s+", "") : "").trim();

                sslWriter.write(java.util.Base64.getEncoder().encodeToString(userClean.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "\r\n");
                sslWriter.flush();
                sslReader.readLine();

                sslWriter.write(java.util.Base64.getEncoder().encodeToString(passClean.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "\r\n");
                sslWriter.flush();
                String authResp = sslReader.readLine();
                log.info("Resilient SMTP Auth response: {}", authResp);
                if (authResp == null || !authResp.startsWith("235")) {
                    throw new java.io.IOException("SMTP Authentication failed: " + authResp);
                }

                sslWriter.write("MAIL FROM:<" + userClean + ">\r\n");
                sslWriter.flush();
                sslReader.readLine();

                List<String> allRecipients = new java.util.ArrayList<>();
                if (toRecipients != null) {
                    for (String r : toRecipients) {
                        if (r != null && !r.isBlank()) allRecipients.add(r.trim());
                    }
                }
                if (ccRecipients != null) {
                    for (String r : ccRecipients) {
                        if (r != null && !r.isBlank()) allRecipients.add(r.trim());
                    }
                }

                for (String recipient : allRecipients) {
                    sslWriter.write("RCPT TO:<" + recipient + ">\r\n");
                    sslWriter.flush();
                    sslReader.readLine();
                }

                sslWriter.write("DATA\r\n");
                sslWriter.flush();
                sslReader.readLine();

                message.writeTo(sslSocket.getOutputStream());
                sslSocket.getOutputStream().write("\r\n.\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                sslSocket.getOutputStream().flush();

                String sendResp = sslReader.readLine();
                log.info("Resilient direct SMTP send response: {}", sendResp);

                sslWriter.write("QUIT\r\n");
                sslWriter.flush();

                if (sendResp != null && sendResp.startsWith("250")) {
                    log.info("Daily OCR Report email successfully delivered via resilient channel to TO: {}, CC: {}", Arrays.toString(toRecipients), Arrays.toString(ccRecipients));
                    return "SUCCESS: Delivered to " + String.join(", ", allRecipients);
                } else {
                    throw new java.io.IOException("SMTP server returned non-250 response: " + sendResp);
                }
            }
        } catch (Exception directEx) {
            log.error("Failed to send daily OCR report via resilient direct channel: {}", directEx.getMessage(), directEx);
            return "FAILED_TO_SEND: " + directEx.getMessage() + " (Report safely stored locally in reports/" + attachmentFileName + ")";
        }
    }

    private String buildHtmlEmailBody(String reportType, String dateStr, Map<String, Object> stats,
                                      Map<String, Long> userWiseCounts, List<VisitingCard> cards,
                                      String attachmentFileName) {
        long totalDocs = stats.get("total") != null ? ((Number) stats.get("total")).longValue() : 0;
        long completedDocs = stats.get("completed") != null ? ((Number) stats.get("completed")).longValue() : 0;
        long failedDocs = stats.get("failed") != null ? ((Number) stats.get("failed")).longValue() : 0;
        double successRate = stats.get("successRate") != null ? ((Number) stats.get("successRate")).doubleValue() : 0.0;

        // Build User-wise summary string and table rows
        StringBuilder userRowsHtml = new StringBuilder();
        StringBuilder userSummaryText = new StringBuilder();

        if (userWiseCounts != null && !userWiseCounts.isEmpty()) {
            int idx = 0;
            for (Map.Entry<String, Long> entry : userWiseCounts.entrySet()) {
                String uploader = entry.getKey();
                Long count = entry.getValue();
                double pct = totalDocs > 0 ? ((double) count / totalDocs) * 100.0 : 0.0;

                if (idx > 0) userSummaryText.append(", ");
                userSummaryText.append(uploader).append(" – ").append(count);

                userRowsHtml.append("""
                    <tr>
                      <td style="padding: 10px 14px; border-bottom: 1px solid #e2e8f0; font-weight: 500; color: #1e293b;">%s</td>
                      <td style="padding: 10px 14px; border-bottom: 1px solid #e2e8f0; text-align: right; font-weight: bold; color: #2563eb;">%d</td>
                      <td style="padding: 10px 14px; border-bottom: 1px solid #e2e8f0; text-align: right; color: #64748b;">%.1f%%</td>
                    </tr>
                    """.formatted(uploader, count, pct));
                idx++;
            }
            if (userSummaryText.length() > 0) {
                userSummaryText.append(", Total – ").append(totalDocs);
            }
        } else {
            userSummaryText.append("Total – ").append(totalDocs);
            userRowsHtml.append("""
                <tr>
                  <td colspan="3" style="padding: 12px 14px; text-align: center; color: #94a3b8;">No uploader data available</td>
                </tr>
                """);
        }

        return """
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
                .summary-line-box { background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 6px; padding: 12px 16px; margin-bottom: 16px; font-size: 13px; color: #334155; }
                .summary-line-title { font-weight: bold; color: #1e3a8a; margin-bottom: 4px; text-transform: uppercase; font-size: 11px; letter-spacing: 0.5px; }
                .user-table { width: 100%%; border-collapse: collapse; margin: 16px 0; border: 1px solid #e2e8f0; border-radius: 6px; overflow: hidden; }
                .user-table th { background: #f1f5f9; padding: 10px 14px; font-size: 12px; font-weight: bold; color: #475569; text-transform: uppercase; border-bottom: 1px solid #e2e8f0; }
                .user-table tfoot td { background: #f8fafc; padding: 10px 14px; font-size: 13px; font-weight: bold; color: #0f172a; border-top: 2px solid #cbd5e1; }
                .stats-table { width: 100%%; border-collapse: separate; border-spacing: 8px 0; margin: 15px 0; }
                .stat-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 12px; text-align: center; }
                .stat-label { font-size: 11px; color: #64748b; text-transform: uppercase; font-weight: bold; }
                .stat-value { font-size: 20px; font-weight: bold; margin-top: 4px; }
                .cta-box { background: #eff6ff; border-left: 4px solid #2563eb; padding: 12px 16px; border-radius: 4px; margin-top: 15px; }
                .footer { text-align: center; padding: 15px; font-size: 11px; color: #94a3b8; border-top: 1px solid #f1f5f9; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2>Global Fintech Fest (GFF) — %s</h2>
                  <p>Date: %s | Report Generated Automatically</p>
                </div>
                <div class="content">
                  <p style="color: #334155; font-size: 14px; margin: 0 0 10px 0;">Hi,</p>
                  <p style="color: #64748b; font-size: 13px; line-height: 1.5; margin: 0 0 15px 0;">
                    Please find below the visiting card OCR upload and processing summary:
                  </p>

                  <!-- User-wise Distribution Table -->
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
            """.formatted(
                reportType,
                dateStr,
                userRowsHtml.toString(),
                totalDocs,
                attachmentFileName
            );
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

    /**
     * Sends an individual visiting card's details to a lead recipient via email.
     */
    public String sendVisitingCardToLead(VisitingCard card, String leadEmail, String subject, String senderName) {
        if (leadEmail == null || leadEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient lead email is required");
        }
        String recipient = leadEmail.trim();
        String cardName = card.getCardHolderName() != null && !card.getCardHolderName().trim().isEmpty()
                ? card.getCardHolderName().trim()
                : (card.getCompanyName() != null ? card.getCompanyName().trim() : card.getRecordId());

        String emailSubject = (subject != null && !subject.trim().isEmpty())
                ? subject.trim()
                : "Visiting Card Details - " + cardName;

        String senderDisplayName = (senderName != null && !senderName.trim().isEmpty()) ? senderName.trim() : "Global Fintech Fest Team";

        MimeMessage message = null;
        if (mailSender != null) {
            try {
                message = mailSender.createMimeMessage();
            } catch (Exception ignored) {}
        }
        if (message == null) {
            message = new MimeMessage(jakarta.mail.Session.getInstance(new java.util.Properties()));
        }

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(recipient);
            helper.setSubject(emailSubject);

            String htmlBody = buildCardShareHtmlBody(card, senderDisplayName);
            helper.setText(htmlBody, true);

            log.info("Dispatching Visiting Card [{}] email to lead: {}", card.getRecordId(), recipient);
            if (mailSender != null) {
                mailSender.send(message);
            } else {
                sendDirectSmtp(message, new String[]{recipient}, "visiting_card_" + card.getRecordId());
            }
            log.info("Visiting Card [{}] successfully shared with lead {}", card.getRecordId(), recipient);
            return "SUCCESS: Shared with " + recipient;
        } catch (Exception e) {
            log.warn("JavaMailSender encountered issue: {}. Activating resilient direct SMTP channel...", e.getMessage());
            return sendDirectSmtp(message, new String[]{recipient}, "visiting_card_" + card.getRecordId());
        }
    }

    private String buildCardShareHtmlBody(VisitingCard card, String senderDisplayName) {
        String name = card.getCardHolderName() != null && !card.getCardHolderName().trim().isEmpty()
                ? card.getCardHolderName() : "N/A";
        String company = card.getCompanyName() != null && !card.getCompanyName().trim().isEmpty()
                ? card.getCompanyName() : "N/A";
        String designation = card.getDesignation() != null && !card.getDesignation().trim().isEmpty()
                ? card.getDesignation() : "N/A";
        String department = card.getDepartment() != null && !card.getDepartment().trim().isEmpty()
                ? card.getDepartment() : null;
        String mobile = card.getExtractedMobile() != null && !card.getExtractedMobile().trim().isEmpty()
                ? card.getExtractedMobile() : "N/A";
        String workNumber = card.getWorkNumber() != null && !card.getWorkNumber().trim().isEmpty()
                ? card.getWorkNumber() : null;
        String email = card.getExtractedEmail() != null && !card.getExtractedEmail().trim().isEmpty()
                ? card.getExtractedEmail() : "N/A";
        String websiteUrl = card.getWebsiteUrl() != null && !card.getWebsiteUrl().trim().isEmpty()
                ? card.getWebsiteUrl() : null;
        String city = card.getCity() != null && !card.getCity().trim().isEmpty()
                ? card.getCity() : null;
        String state = card.getState() != null && !card.getState().trim().isEmpty()
                ? card.getState() : null;
        String postalZipCode = card.getPostalZipCode() != null && !card.getPostalZipCode().trim().isEmpty()
                ? card.getPostalZipCode() : null;
        String country = card.getCountry() != null && !card.getCountry().trim().isEmpty()
                ? card.getCountry() : null;
        String linkedIn = card.getLinkedIn() != null && !card.getLinkedIn().trim().isEmpty()
                ? card.getLinkedIn() : null;
        String twitter = card.getTwitter() != null && !card.getTwitter().trim().isEmpty()
                ? card.getTwitter() : null;
        String address = card.getExtractedAddress() != null && !card.getExtractedAddress().trim().isEmpty()
                ? card.getExtractedAddress() : null;
        String notes = card.getNotes() != null && !card.getNotes().trim().isEmpty()
                ? card.getNotes() : null;
        String imageUrl = card.getImageUrl() != null && !card.getImageUrl().trim().isEmpty()
                ? card.getImageUrl() : null;

        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
                .header { background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%); color: #ffffff; padding: 24px 28px; }
                .header h1 { margin: 0; font-size: 20px; font-weight: 700; letter-spacing: -0.02em; }
                .header p { margin: 6px 0 0 0; font-size: 13px; opacity: 0.85; }
                .content { padding: 28px; }
                .card-box { background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 10px; padding: 20px; margin-bottom: 20px; }
                .card-title { font-size: 18px; font-weight: 700; color: #0f172a; margin-bottom: 2px; }
                .card-subtitle { font-size: 13px; color: #2563eb; font-weight: 600; margin-bottom: 16px; }
                .field-row td { padding: 6px 0; font-size: 13px; }
                .field-label { color: #64748b; font-weight: 600; width: 120px; }
                .field-value { color: #0f172a; font-weight: 500; }
                .image-box { text-align: center; margin: 20px 0 10px 0; }
                .image-box img { max-width: 100%; max-height: 240px; border-radius: 8px; border: 1px solid #cbd5e1; object-fit: contain; }
                .footer { text-align: center; padding: 18px 24px; font-size: 12px; color: #64748b; background: #f8fafc; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Visiting Card Shared With You</h1>
                  <p>Global Fintech Fest — Official Contact Details</p>
                </div>
                <div class="content">
                  <p style="margin-top: 0; font-size: 14px; line-height: 1.5;">Hello,</p>
                  <p style="font-size: 14px; line-height: 1.5; color: #334155;">
                    Please find the verified contact and business details for the visiting card below:
                  </p>
                  <div class="card-box">
                    <div class="card-title">""").append(name).append("""
                    </div>
                    <div class="card-subtitle">""").append(designation).append(" • ").append(company).append("""
                    </div>
                    <table style="width: 100%; border-collapse: collapse;">
                      <tr class="field-row"><td class="field-label">Company:</td><td class="field-value">""").append(company).append("""
                      </td></tr>
                      <tr class="field-row"><td class="field-label">Job Title:</td><td class="field-value">""").append(designation).append("""
                      </td></tr>
            """);

        if (department != null) {
            html.append("""
                      <tr class="field-row"><td class="field-label">Department:</td><td class="field-value">""").append(department).append("""
                      </td></tr>
            """);
        }

        html.append("""
                      <tr class="field-row"><td class="field-label">Email:</td><td class="field-value" style="color: #2563eb;"><a href="mailto: """).append(email).append("""
                      " style="color: #2563eb; text-decoration: none;">""").append(email).append("""
                      </a></td></tr>
                      <tr class="field-row"><td class="field-label">Mobile Number:</td><td class="field-value" style="font-family: monospace;">""").append(mobile).append("""
                      </td></tr>
            """);

        if (workNumber != null) {
            html.append("""
                      <tr class="field-row"><td class="field-label">Work Number:</td><td class="field-value" style="font-family: monospace;">""").append(workNumber).append("""
                      </td></tr>
            """);
        }

        if (websiteUrl != null) {
            html.append("""
                      <tr class="field-row"><td class="field-label">Website:</td><td class="field-value"><a href=\"""").append(websiteUrl.startsWith("http") ? websiteUrl : "https://" + websiteUrl).append("""
                      \" style="color: #2563eb;" target="_blank">""").append(websiteUrl).append("""
                      </a></td></tr>
            """);
        }

        if (city != null || state != null || postalZipCode != null || country != null) {
            String locationStr = String.join(", ", Arrays.asList(city, state, postalZipCode, country).stream().filter(s -> s != null && !s.isEmpty()).toList());
            html.append("""
                      <tr class="field-row"><td class="field-label">Location:</td><td class="field-value">""").append(locationStr).append("""
                      </td></tr>
            """);
        }

        if (address != null) {
            html.append("""
                      <tr class="field-row"><td class="field-label">Full Address:</td><td class="field-value">""").append(address).append("""
                      </td></tr>
            """);
        }

        if (linkedIn != null) {
            html.append("""
                      <tr class="field-row"><td class="field-label">LinkedIn:</td><td class="field-value"><a href=\"""").append(linkedIn.startsWith("http") ? linkedIn : "https://" + linkedIn).append("""
                      \" style="color: #2563eb;" target="_blank">""").append(linkedIn).append("""
                      </a></td></tr>
            """);
        }

        if (twitter != null) {
            html.append("""
                      <tr class="field-row"><td class="field-label">Twitter / X:</td><td class="field-value"><a href=\"""").append(twitter.startsWith("http") ? twitter : "https://" + twitter).append("""
                      \" style="color: #2563eb;" target="_blank">""").append(twitter).append("""
                      </a></td></tr>
            """);
        }

        html.append("""
                    </table>
                  </div>
            """);

        if (notes != null) {
            html.append("""
                  <div style="background: #fffbeb; border: 1px solid #fef3c7; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px; font-size: 13px; color: #92400e;">
                    <strong>Notes:</strong> """).append(notes).append("""
                  </div>
            """);
        }

        if (imageUrl != null && imageUrl.startsWith("http")) {
            html.append("""
                  <div class="image-box">
                    <p style="font-size: 12px; color: #64748b; margin-bottom: 8px; font-weight: 600;">Card Image Preview:</p>
                    <img src=\"""").append(imageUrl).append("""
                    \" alt="Visiting Card" />
                  </div>
            """);
        }

        html.append("""
                  <p style="font-size: 13px; color: #64748b; margin-bottom: 0;">
                    Regards,<br/>
                    <strong>""").append(senderDisplayName).append("""
                    </strong>
                  </p>
                </div>
                <div class="footer">
                  This email was dispatched on behalf of <strong>""").append(senderDisplayName).append("""
                  </strong> via the Global Fintech Fest FieldCapture Portal.<br/>
                  Confidential &copy; 2026 Global Fintech Fest. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """);

        return html.toString();
    }
}
