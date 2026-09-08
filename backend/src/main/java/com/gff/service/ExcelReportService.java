package com.gff.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gff.entity.VisitingCard;
import com.gff.entity.enums.OcrStatus;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to generate rich, formatted Excel (.xlsx) spreadsheets using Apache POI.
 * Creates a "Summary" sheet and an "All Documents" sheet with all 14 standardized visiting card fields.
 */
@Service
public class ExcelReportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelReportService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates a two-sheet Excel report as an in-memory byte array.
     *
     * @param documents    List of visiting card documents uploaded today
     * @param reportDate   Date of report
     * @param summaryStats Calculated metrics
     * @return Raw byte array of .xlsx workbook
     */
    public byte[] generateDailyOcrReport(List<VisitingCard> documents, LocalDate reportDate, Map<String, Object> summaryStats) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CreationHelper createHelper = workbook.getCreationHelper();

            // Reusable cell styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle summaryTitleStyle = createSummaryTitleStyle(workbook);
            CellStyle metricLabelStyle = createMetricLabelStyle(workbook);
            CellStyle metricValueStyle = createMetricValueStyle(workbook);
            CellStyle dataRowStyle = createDataRowStyle(workbook);
            CellStyle completedStatusStyle = createStatusStyle(workbook, IndexedColors.GREEN.getIndex());
            CellStyle failedStatusStyle = createStatusStyle(workbook, IndexedColors.RED.getIndex());
            CellStyle hyperlinkStyle = createHyperlinkStyle(workbook);

            // ==========================================
            // SHEET 1: Summary
            // ==========================================
            Sheet summarySheet = workbook.createSheet("Summary");
            summarySheet.setDisplayGridlines(true);

            int sumRowIdx = 0;

            // Title
            Row titleRow = summarySheet.createRow(sumRowIdx++);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Global Fintech Fest (GFF) - Daily OCR & Upload Summary");
            titleCell.setCellStyle(summaryTitleStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            // Date Subtitle
            Row dateRow = summarySheet.createRow(sumRowIdx++);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue("Report Generated For: " + reportDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
            summarySheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));

            sumRowIdx++; // Blank spacing row

            // Metrics Table Header
            Row metricsHeaderRow = summarySheet.createRow(sumRowIdx++);
            metricsHeaderRow.setHeightInPoints(22);
            Cell mhc1 = metricsHeaderRow.createCell(0);
            mhc1.setCellValue("Metric Description");
            mhc1.setCellStyle(headerStyle);
            Cell mhc2 = metricsHeaderRow.createCell(1);
            mhc2.setCellValue("Count / Value");
            mhc2.setCellStyle(headerStyle);

            long total = summaryStats.get("total") != null ? ((Number) summaryStats.get("total")).longValue() : 0;
            long completed = summaryStats.get("completed") != null ? ((Number) summaryStats.get("completed")).longValue() : 0;
            long failed = summaryStats.get("failed") != null ? ((Number) summaryStats.get("failed")).longValue() : 0;
            double successRate = summaryStats.get("successRate") != null ? ((Number) summaryStats.get("successRate")).doubleValue() : 0.0;

            Object[][] metrics = {
                    {"Total Documents Uploaded", total},
                    {"Successfully Processed (OCR Completed)", completed},
                    {"Failed OCR / Flagged for Review", failed},
                    {"Success Rate (%)", String.format("%.1f%%", successRate)}
            };

            for (Object[] metric : metrics) {
                Row row = summarySheet.createRow(sumRowIdx++);
                row.setHeightInPoints(18);
                Cell labelCell = row.createCell(0);
                labelCell.setCellValue(metric[0].toString());
                labelCell.setCellStyle(metricLabelStyle);

                Cell valueCell = row.createCell(1);
                valueCell.setCellValue(metric[1].toString());
                valueCell.setCellStyle(metricValueStyle);
            }

            sumRowIdx++; // Spacing

            // Section Title for Extracted Visiting Cards
            Row cardsSecRow = summarySheet.createRow(sumRowIdx++);
            cardsSecRow.setHeightInPoints(24);
            Cell secCell = cardsSecRow.createCell(0);
            secCell.setCellValue("Extracted Visiting Cards - OCR Data Summary");
            secCell.setCellStyle(summaryTitleStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(sumRowIdx - 1, sumRowIdx - 1, 0, 15));

            // Tabular Header on Sheet 1 (Standardized 14 Fields + Meta)
            String[] summaryTableHeaders = {
                    "#", "Name", "Job Title", "Company Name", "Department",
                    "Email Address", "Mobile Number", "Work Number", "Website URL",
                    "City", "State", "Postal/ZIP Code", "Country", "LinkedIn", "Twitter/X",
                    "OCR Status", "Uploaded By"
            };
            Row sumTblHeader = summarySheet.createRow(sumRowIdx++);
            sumTblHeader.setHeightInPoints(24);
            for (int i = 0; i < summaryTableHeaders.length; i++) {
                Cell c = sumTblHeader.createCell(i);
                c.setCellValue(summaryTableHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            int cardIdx = 1;
            for (VisitingCard card : documents) {
                Row row = summarySheet.createRow(sumRowIdx++);
                row.setHeightInPoints(20);

                int col = 0;
                Cell c0 = row.createCell(col++); c0.setCellValue(cardIdx++); c0.setCellStyle(dataRowStyle);
                Cell c1 = row.createCell(col++); c1.setCellValue(card.getCardHolderName() != null ? card.getCardHolderName() : "N/A"); c1.setCellStyle(dataRowStyle);
                Cell c2 = row.createCell(col++); c2.setCellValue(card.getDesignation() != null ? card.getDesignation() : "N/A"); c2.setCellStyle(dataRowStyle);
                Cell c3 = row.createCell(col++); c3.setCellValue(card.getCompanyName() != null ? card.getCompanyName() : "N/A"); c3.setCellStyle(dataRowStyle);
                Cell c4 = row.createCell(col++); c4.setCellValue(card.getDepartment() != null ? card.getDepartment() : "N/A"); c4.setCellStyle(dataRowStyle);
                Cell c5 = row.createCell(col++); c5.setCellValue(card.getExtractedEmail() != null ? card.getExtractedEmail() : "N/A"); c5.setCellStyle(dataRowStyle);
                Cell c6 = row.createCell(col++); c6.setCellValue(card.getExtractedMobile() != null ? card.getExtractedMobile() : "N/A"); c6.setCellStyle(dataRowStyle);
                Cell c7 = row.createCell(col++); c7.setCellValue(card.getWorkNumber() != null ? card.getWorkNumber() : "N/A"); c7.setCellStyle(dataRowStyle);
                Cell c8 = row.createCell(col++); c8.setCellValue(card.getWebsiteUrl() != null ? card.getWebsiteUrl() : "N/A"); c8.setCellStyle(dataRowStyle);
                Cell c9 = row.createCell(col++); c9.setCellValue(card.getCity() != null ? card.getCity() : "N/A"); c9.setCellStyle(dataRowStyle);
                Cell c10 = row.createCell(col++); c10.setCellValue(card.getState() != null ? card.getState() : "N/A"); c10.setCellStyle(dataRowStyle);
                Cell c11 = row.createCell(col++); c11.setCellValue(card.getPostalZipCode() != null ? card.getPostalZipCode() : "N/A"); c11.setCellStyle(dataRowStyle);
                Cell c12 = row.createCell(col++); c12.setCellValue(card.getCountry() != null ? card.getCountry() : "N/A"); c12.setCellStyle(dataRowStyle);
                Cell c13 = row.createCell(col++); c13.setCellValue(card.getLinkedIn() != null ? card.getLinkedIn() : "N/A"); c13.setCellStyle(dataRowStyle);
                Cell c14 = row.createCell(col++); c14.setCellValue(card.getTwitter() != null ? card.getTwitter() : "N/A"); c14.setCellStyle(dataRowStyle);
                
                Cell c15 = row.createCell(col++);
                c15.setCellValue(card.getOcrStatus() != null ? card.getOcrStatus().name() : "UNKNOWN");
                c15.setCellStyle(card.getOcrStatus() == OcrStatus.COMPLETED ? completedStatusStyle : failedStatusStyle);

                Cell c16 = row.createCell(col++); c16.setCellValue(card.getUploaderEmail() != null ? card.getUploaderEmail() : "N/A"); c16.setCellStyle(dataRowStyle);
            }

            for (int i = 0; i < summaryTableHeaders.length; i++) {
                summarySheet.autoSizeColumn(i);
            }

            // ==========================================
            // SHEET 2: All Documents (14 Standardized Fields + Full Meta)
            // ==========================================
            Sheet docSheet = workbook.createSheet("All Documents");
            docSheet.setDisplayGridlines(true);

            String[] columns = {
                    "Document ID",
                    "Name",
                    "Job Title",
                    "Company Name",
                    "Department",
                    "Email Address",
                    "Mobile Number",
                    "Work Number",
                    "Website URL",
                    "City",
                    "State",
                    "Postal/ZIP Code",
                    "Country",
                    "LinkedIn",
                    "Twitter/X",
                    "Address",
                    "Status",
                    "Uploaded By",
                    "S3 File URL",
                    "Uploaded At"
            };

            Row docHeaderRow = docSheet.createRow(0);
            docHeaderRow.setHeightInPoints(26);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = docHeaderRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int docRowIdx = 1;
            for (VisitingCard card : documents) {
                Row row = docSheet.createRow(docRowIdx++);
                row.setHeightInPoints(20);
                int col = 0;

                // 0. Document ID
                Cell c0 = row.createCell(col++);
                c0.setCellValue(card.getRecordId() != null ? card.getRecordId() : String.valueOf(card.getId()));
                c0.setCellStyle(dataRowStyle);

                // 1. Name
                Cell c1 = row.createCell(col++);
                c1.setCellValue(card.getCardHolderName() != null ? card.getCardHolderName() : "N/A");
                c1.setCellStyle(dataRowStyle);

                // 2. Job Title
                Cell c2 = row.createCell(col++);
                c2.setCellValue(card.getDesignation() != null ? card.getDesignation() : "N/A");
                c2.setCellStyle(dataRowStyle);

                // 3. Company Name
                Cell c3 = row.createCell(col++);
                c3.setCellValue(card.getCompanyName() != null ? card.getCompanyName() : "N/A");
                c3.setCellStyle(dataRowStyle);

                // 4. Department
                Cell c4 = row.createCell(col++);
                c4.setCellValue(card.getDepartment() != null ? card.getDepartment() : "N/A");
                c4.setCellStyle(dataRowStyle);

                // 5. Email Address
                Cell c5 = row.createCell(col++);
                c5.setCellValue(card.getExtractedEmail() != null ? card.getExtractedEmail() : "N/A");
                c5.setCellStyle(dataRowStyle);

                // 6. Mobile Number
                Cell c6 = row.createCell(col++);
                c6.setCellValue(card.getExtractedMobile() != null ? card.getExtractedMobile() : "N/A");
                c6.setCellStyle(dataRowStyle);

                // 7. Work Number
                Cell c7 = row.createCell(col++);
                c7.setCellValue(card.getWorkNumber() != null ? card.getWorkNumber() : "N/A");
                c7.setCellStyle(dataRowStyle);

                // 8. Website URL
                Cell c8 = row.createCell(col++);
                c8.setCellValue(card.getWebsiteUrl() != null ? card.getWebsiteUrl() : "N/A");
                c8.setCellStyle(dataRowStyle);

                // 9. City
                Cell c9 = row.createCell(col++);
                c9.setCellValue(card.getCity() != null ? card.getCity() : "N/A");
                c9.setCellStyle(dataRowStyle);

                // 10. State
                Cell c10 = row.createCell(col++);
                c10.setCellValue(card.getState() != null ? card.getState() : "N/A");
                c10.setCellStyle(dataRowStyle);

                // 11. Postal/ZIP Code
                Cell c11 = row.createCell(col++);
                c11.setCellValue(card.getPostalZipCode() != null ? card.getPostalZipCode() : "N/A");
                c11.setCellStyle(dataRowStyle);

                // 12. Country
                Cell c12 = row.createCell(col++);
                c12.setCellValue(card.getCountry() != null ? card.getCountry() : "N/A");
                c12.setCellStyle(dataRowStyle);

                // 13. LinkedIn
                Cell c13 = row.createCell(col++);
                c13.setCellValue(card.getLinkedIn() != null ? card.getLinkedIn() : "N/A");
                c13.setCellStyle(dataRowStyle);

                // 14. Twitter/X
                Cell c14 = row.createCell(col++);
                c14.setCellValue(card.getTwitter() != null ? card.getTwitter() : "N/A");
                c14.setCellStyle(dataRowStyle);

                // 15. Address
                Cell c15 = row.createCell(col++);
                c15.setCellValue(card.getExtractedAddress() != null ? card.getExtractedAddress() : "N/A");
                c15.setCellStyle(dataRowStyle);

                // 16. Status (COMPLETED in Green, FAILED in Red)
                Cell c16 = row.createCell(col++);
                OcrStatus ocrStatus = card.getOcrStatus() != null ? card.getOcrStatus() : OcrStatus.PENDING;
                c16.setCellValue(ocrStatus.name());
                if (ocrStatus == OcrStatus.COMPLETED) {
                    c16.setCellStyle(completedStatusStyle);
                } else if (ocrStatus == OcrStatus.FAILED) {
                    c16.setCellStyle(failedStatusStyle);
                } else {
                    c16.setCellStyle(dataRowStyle);
                }

                // 17. Uploaded By
                Cell c17 = row.createCell(col++);
                c17.setCellValue(card.getUploaderEmail() != null ? card.getUploaderEmail() : "N/A");
                c17.setCellStyle(dataRowStyle);

                // 18. S3 File URL (Clickable Hyperlink)
                Cell c18 = row.createCell(col++);
                String s3Url = card.getImageUrl() != null ? card.getImageUrl() : "";
                c18.setCellValue(s3Url.isEmpty() ? "N/A" : "View Image");
                if (!s3Url.isEmpty()) {
                    try {
                        Hyperlink link = createHelper.createHyperlink(HyperlinkType.URL);
                        link.setAddress(s3Url);
                        c18.setHyperlink(link);
                        c18.setCellStyle(hyperlinkStyle);
                    } catch (Exception e) {
                        c18.setCellStyle(dataRowStyle);
                    }
                } else {
                    c18.setCellStyle(dataRowStyle);
                }

                // 19. Uploaded At
                Cell c19 = row.createCell(col++);
                c19.setCellValue(card.getCreatedAt() != null ? card.getCreatedAt().format(DATE_TIME_FORMATTER) : "N/A");
                c19.setCellStyle(dataRowStyle);
            }

            // Apply Auto-Filter to All Documents Sheet
            if (docRowIdx > 1) {
                docSheet.setAutoFilter(new CellRangeAddress(0, docRowIdx - 1, 0, columns.length - 1));
            }

            // Auto-fit all column widths
            for (int i = 0; i < columns.length; i++) {
                docSheet.autoSizeColumn(i);
                int currentWidth = docSheet.getColumnWidth(i);
                docSheet.setColumnWidth(i, Math.min(currentWidth + 1200, 20000));
            }

            workbook.write(out);
            log.info("Successfully generated Excel report (.xlsx) for {} documents with 14 standardized fields (size: {} bytes)",
                    documents.size(), out.size());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate Excel daily report: {}", e.getMessage(), e);
            throw new RuntimeException("Excel report generation error: " + e.getMessage(), e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createSummaryTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createMetricLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createMetricValueStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataRowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createStatusStyle(Workbook workbook, short colorIndex) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        font.setColor(colorIndex);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createHyperlinkStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setUnderline(Font.U_SINGLE);
        font.setColor(IndexedColors.BLUE.getIndex());
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
