package com.doctorservice.service;

import com.doctorservice.config.PdfConfig;
import com.doctorservice.entity.ConsultationReport;
import com.doctorservice.entity.Doctor;
import com.doctorservice.entity.Appointment;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PdfGenerationService {

    @Autowired
    private PdfConfig pdfConfig;

    @Autowired
    private FileStorageService fileStorageService;

    // Font definitions
    private static final Font FONT_TITLE = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font FONT_HEADING = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLACK);
    private static final Font FONT_SUBHEADING = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLACK);
    private static final Font FONT_NORMAL = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
    private static final Font FONT_SMALL = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);
    private static final Font FONT_LABEL = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.DARK_GRAY);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Generate PDF for consultation report
     * The file will be encrypted and compressed before storage
     * @param report The consultation report entity
     * @return URL of the generated PDF file
     */
    public String generateReportPdf(ConsultationReport report) {
        try {
            log.info("Starting PDF generation for report ID: {}", report.getId());

            // Generate PDF content
            byte[] pdfContent = createPdfDocument(report);

            // Generate filename
            String filename = generateFilename(report);

            // Save to storage (FileStorageService will handle encryption)
            String fileUrl = fileStorageService.saveFile(pdfContent, filename, report.getDoctor().getId(),
                    report.getPatientId(), report.getCaseId());

            log.info("PDF generated and encrypted successfully for report ID: {}. URL: {}",
                    report.getId(), fileUrl);

            return fileUrl;

        } catch (Exception ex) {
            log.error("Failed to generate PDF for report ID: {}", report.getId(), ex);
            throw new RuntimeException("Failed to generate PDF: " + ex.getMessage(), ex);
        }
    }

    /**
     * Create the actual PDF document
     */
    private byte[] createPdfDocument(ConsultationReport report) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Create document with A4 page size
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            // Add metadata
            document.addTitle("Medical Consultation Report");
            document.addAuthor("Medical Consultation System");
            document.addCreator("MCS PDF Generator");
            document.addSubject("Consultation Report #" + report.getId());
            document.addCreationDate();

            // Add header and footer
            writer.setPageEvent(new HeaderFooterPageEvent(report));

            document.open();

            // Add content sections
            addReportHeader(document, report);
            addSeparator(document);

            addPatientInformation(document, report);
            addSeparator(document);

            addDoctorInformation(document, report);
            addSeparator(document);

            addAppointmentInformation(document, report);
            addSeparator(document);

            addMedicalInformation(document, report);
            addSeparator(document);

            addRecommendationsAndFollowUp(document, report);

            if (report.getDoctorNotes() != null && !report.getDoctorNotes().trim().isEmpty()) {
                addSeparator(document);
                addDoctorNotes(document, report);
            }

            addSeparator(document);
            addFooterInformation(document, report);

            addEncryptionNotice(document);

            document.close();
            writer.close();

            log.info("PDF document created successfully. Size: {} bytes", baos.size());

            return baos.toByteArray();

        } catch (DocumentException ex) {
            log.error("Error creating PDF document", ex);
            throw ex;
        }
    }

    // ========== CONTENT SECTIONS ==========

    private void addReportHeader(Document document, ConsultationReport report) throws DocumentException {
        // Title
        Paragraph title = new Paragraph("MEDICAL CONSULTATION REPORT", FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        // Report metadata
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setSpacingAfter(15);

        addMetaCell(metaTable, "Report ID:", "#" + report.getId());
        addMetaCell(metaTable, "Case ID:", "#" + report.getCaseId());
        addMetaCell(metaTable, "Generated Date:",
                report.getCreatedAt().format(DATETIME_FORMATTER));
        addMetaCell(metaTable, "Status:", "FINALIZED");

        document.add(metaTable);
    }

    private void addPatientInformation(Document document, ConsultationReport report)
            throws DocumentException {

        addSectionTitle(document, "PATIENT INFORMATION");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new float[]{1, 2});

        addLabelValueRow(table, "Patient ID:", "Patient #" + report.getAppointment().getPatientId());

        document.add(table);
    }

    private void addDoctorInformation(Document document, ConsultationReport report)
            throws DocumentException {

        addSectionTitle(document, "ATTENDING PHYSICIAN");

        Doctor doctor = report.getDoctor();

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new float[]{1, 2});

        addLabelValueRow(table, "Doctor Name:", doctor.getFullName());
        addLabelValueRow(table, "License Number:", doctor.getLicenseNumber());
        addLabelValueRow(table, "Specialization:", doctor.getPrimarySpecialization());

        if (doctor.getPhoneNumber() != null) {
            addLabelValueRow(table, "Contact:", doctor.getPhoneNumber());
        }

        if (doctor.getHospitalAffiliation() != null) {
            addLabelValueRow(table, "Affiliation:", doctor.getHospitalAffiliation());
        }

        document.add(table);
    }

    private void addAppointmentInformation(Document document, ConsultationReport report)
            throws DocumentException {

        addSectionTitle(document, "CONSULTATION DETAILS");

        Appointment appointment = report.getAppointment();

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new float[]{1, 2});

        addLabelValueRow(table, "Appointment ID:", "#" + appointment.getId());
        addLabelValueRow(table, "Date & Time:",
                appointment.getScheduledTime().format(DATETIME_FORMATTER));
        addLabelValueRow(table, "Duration:", appointment.getDuration() + " minutes");
        addLabelValueRow(table, "Type:", appointment.getConsultationType().toString());

        document.add(table);
    }

    private void addMedicalInformation(Document document, ConsultationReport report)
            throws DocumentException {

        addSectionTitle(document, "MEDICAL FINDINGS");

        // Diagnosis
        if (report.getDiagnosis() != null && !report.getDiagnosis().trim().isEmpty()) {
            addSubsectionTitle(document, "Diagnosis:");
            Paragraph diagnosis = new Paragraph(report.getDiagnosis(), FONT_NORMAL);
            diagnosis.setAlignment(Element.ALIGN_JUSTIFIED);
            diagnosis.setSpacingAfter(10);
            document.add(diagnosis);
        }

        // Prescriptions
        if (report.getPrescriptions() != null && !report.getPrescriptions().trim().isEmpty()) {
            addSubsectionTitle(document, "Prescriptions:");
            Paragraph prescriptions = new Paragraph(report.getPrescriptions(), FONT_NORMAL);
            prescriptions.setAlignment(Element.ALIGN_JUSTIFIED);
            prescriptions.setSpacingAfter(10);
            document.add(prescriptions);
        }
    }

    private void addRecommendationsAndFollowUp(Document document, ConsultationReport report)
            throws DocumentException {

        addSectionTitle(document, "RECOMMENDATIONS & FOLLOW-UP");

        // Recommendations
        if (report.getRecommendations() != null && !report.getRecommendations().trim().isEmpty()) {
            addSubsectionTitle(document, "Medical Recommendations:");
            Paragraph recommendations = new Paragraph(report.getRecommendations(), FONT_NORMAL);
            recommendations.setAlignment(Element.ALIGN_JUSTIFIED);
            recommendations.setSpacingAfter(10);
            document.add(recommendations);
        }

        // Follow-up instructions
        if (report.getFollowUpInstructions() != null &&
                !report.getFollowUpInstructions().trim().isEmpty()) {
            addSubsectionTitle(document, "Follow-up Instructions:");
            Paragraph followUp = new Paragraph(report.getFollowUpInstructions(), FONT_NORMAL);
            followUp.setAlignment(Element.ALIGN_JUSTIFIED);
            followUp.setSpacingAfter(10);
            document.add(followUp);
        }

        // Follow-up requirement
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new float[]{1, 2});

        addLabelValueRow(table, "Follow-up Required:",
                report.getRequiresFollowUp() ? "Yes" : "No");

        if (report.getRequiresFollowUp() && report.getNextAppointmentSuggested() != null) {
            addLabelValueRow(table, "Next Appointment:",
                    report.getNextAppointmentSuggested().format(DATETIME_FORMATTER));
        }

        document.add(table);
    }

    private void addDoctorNotes(Document document, ConsultationReport report)
            throws DocumentException {

        addSectionTitle(document, "DOCTOR'S NOTES (CONFIDENTIAL)");

        Paragraph notes = new Paragraph(report.getDoctorNotes(), FONT_NORMAL);
        notes.setAlignment(Element.ALIGN_JUSTIFIED);
        notes.setSpacingAfter(10);
        document.add(notes);
    }

    private void addFooterInformation(Document document, ConsultationReport report)
            throws DocumentException {

        Paragraph disclaimer = new Paragraph(
                "This is an electronically generated medical consultation report. " +
                        "This document is confidential and intended solely for the named patient. " +
                        "Unauthorized disclosure or distribution is prohibited.",
                FONT_SMALL
        );
        disclaimer.setAlignment(Element.ALIGN_JUSTIFIED);
        disclaimer.setSpacingBefore(20);
        document.add(disclaimer);

        // Digital signature placeholder
        Paragraph signature = new Paragraph("\n\n" +
                "___________________________\n" +
                "Digital Signature\n" +
                report.getDoctor().getFullName() + "\n" +
                "License: " + report.getDoctor().getLicenseNumber(),
                FONT_SMALL
        );
        signature.setAlignment(Element.ALIGN_RIGHT);
        signature.setSpacingBefore(20);
        document.add(signature);
    }

    private void addEncryptionNotice(Document document) throws DocumentException {
        Paragraph encryptionNotice = new Paragraph(
                "\n⚠ SECURITY NOTICE: This PDF file is stored with AES encryption and GZIP compression for enhanced security and confidentiality.",
                new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC, BaseColor.GRAY)
        );
        encryptionNotice.setAlignment(Element.ALIGN_CENTER);
        encryptionNotice.setSpacingBefore(10);
        document.add(encryptionNotice);
    }

    // ========== HELPER METHODS ==========

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph section = new Paragraph(title, FONT_HEADING);
        section.setSpacingBefore(10);
        section.setSpacingAfter(8);
        document.add(section);
    }

    private void addSubsectionTitle(Document document, String title) throws DocumentException {
        Paragraph subsection = new Paragraph(title, FONT_SUBHEADING);
        subsection.setSpacingBefore(5);
        subsection.setSpacingAfter(5);
        document.add(subsection);
    }

    private void addSeparator(Document document) throws DocumentException {
        LineSeparator separator = new LineSeparator();
        separator.setLineColor(BaseColor.LIGHT_GRAY);
        Chunk linebreak = new Chunk(separator);
        Paragraph p = new Paragraph(linebreak);
        p.setSpacingBefore(10);
        p.setSpacingAfter(10);
        document.add(p);
    }

    private void addMetaCell(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_LABEL));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_NORMAL));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void addLabelValueRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_LABEL));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(new BaseColor(245, 245, 245));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A", FONT_NORMAL));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private String generateFilename(ConsultationReport report) {
        String timestamp = report.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("medical_report_case%d_report%d_%s.pdf",
                report.getCaseId(),
                report.getId(),
                timestamp);
    }

    // ========== HEADER/FOOTER PAGE EVENT ==========

    /**
     * Custom page event for adding headers and footers
     */
    private static class HeaderFooterPageEvent extends PdfPageEventHelper {
        private final ConsultationReport report;

        public HeaderFooterPageEvent(ConsultationReport report) {
            this.report = report;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            // Header
            Phrase header = new Phrase("Medical Consultation System - Confidential & Encrypted Document", FONT_SMALL);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    header,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.top() + 20,
                    0);

            // Footer
            Phrase footer = new Phrase(
                    String.format("Page %d | Report ID: %d | Generated: %s | 🔒 Encrypted",
                            writer.getPageNumber(),
                            report.getId(),
                            report.getCreatedAt().format(DATE_FORMATTER)),
                    FONT_SMALL
            );
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    footer,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - 20,
                    0);
        }
    }
}