package za.ac.alis.legal.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import za.ac.alis.core.enums.RiskLevel;
import za.ac.alis.core.persistence.Clause;
import za.ac.alis.core.persistence.Client;
import za.ac.alis.core.persistence.SummaryReport;
import za.ac.alis.legal.persistence.ClauseRepository;

@Service
public class PdfReportService {

    private static final DeviceRgb BRAND = new DeviceRgb(20, 58, 86);
    private static final DeviceRgb HIGH = new DeviceRgb(185, 28, 28);
    private static final DeviceRgb MEDIUM = new DeviceRgb(217, 119, 6);
    private static final DeviceRgb LOW = new DeviceRgb(22, 101, 52);
    private static final DeviceRgb SOFT_HIGH = new DeviceRgb(254, 226, 226);
    private static final DeviceRgb SOFT_MEDIUM = new DeviceRgb(254, 243, 199);
    private static final DeviceRgb SOFT_LOW = new DeviceRgb(220, 252, 231);
    private static final DeviceRgb MUTED = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb LIGHT = new DeviceRgb(241, 245, 249);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ClauseRepository clauseRepository;

    public PdfReportService(ClauseRepository clauseRepository) {
        this.clauseRepository = clauseRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generateComplianceReportPdf(SummaryReport report) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(output);
             PdfDocument pdfDocument = new PdfDocument(writer);
             Document pdf = new Document(pdfDocument, PageSize.A4)) {

            pdf.setMargins(42, 42, 42, 42);
            List<Clause> clauses = loadClauses(report);

            addCoverPage(pdf, report, clauses);
            pdf.add(new AreaBreak());
            addRiskSummary(pdf, report, clauses);
            addAiSection(pdf, "AI Explanation", valueOrFallback(report.getAiExplanation()));
            addAiSection(pdf, "AI Recommendation", valueOrFallback(report.getAiRecommendation()));
            addClauseSection(pdf, clauses);
            addFooter(pdf);
        }

        return output.toByteArray();
    }

    private List<Clause> loadClauses(SummaryReport report) {
        if (report.getDocument() == null || report.getDocument().getDocumentId() == null) {
            return List.of();
        }
        return clauseRepository.findByDocument_DocumentId(report.getDocument().getDocumentId());
    }

    private void addCoverPage(Document pdf, SummaryReport report, List<Clause> clauses) {
        pdf.add(new Paragraph("ALIS")
                .setFontSize(18)
                .setBold()
                .setFontColor(BRAND)
                .setTextAlignment(TextAlignment.CENTER));
        pdf.add(new Paragraph("Compliance Analysis Report")
                .setFontSize(26)
                .setBold()
                .setFontColor(ColorConstants.BLACK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(70)
                .setMarginBottom(16));

        pdf.add(riskBadge(report.getRiskLevel())
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                .setMarginBottom(30));

        pdf.add(clientInfoTable(report, clauses)
                .setMarginTop(28)
                .setMarginBottom(28));

        pdf.add(new Paragraph("Generated report artifact")
                .setFontSize(10)
                .setFontColor(MUTED)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(70));
        pdf.add(new Paragraph("Confidential - not legal advice")
                .setFontSize(9)
                .setFontColor(MUTED)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private Table clientInfoTable(SummaryReport report, List<Clause> clauses) {
        Table table = new Table(UnitValue.createPercentArray(new float[] { 32, 68 }))
                .useAllAvailableWidth();

        addInfoRow(table, "Client", clientName(report.getClient()));
        addInfoRow(table, "Client ID", id(report.getClient() != null ? report.getClient().getClientId() : null));
        addInfoRow(table, "Document", report.getDocument() != null ? safe(report.getDocument().getTitle()) : "N/A");
        addInfoRow(table, "Document ID", id(report.getDocument() != null ? report.getDocument().getDocumentId() : null));
        addInfoRow(table, "Generated", report.getGeneratedAt() != null ? DATE_TIME.format(report.getGeneratedAt()) : "N/A");
        addInfoRow(table, "Model", safe(report.getModelVersion()));
        addInfoRow(table, "Matched Act", lawName(report));
        addInfoRow(table, "Similarity", similarity(report.getSimilarityScore()));
        addInfoRow(table, "Clauses Reviewed", String.valueOf(clauses.size()));

        return table;
    }

    private void addInfoRow(Table table, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setBold().setFontSize(10).setFontColor(MUTED))
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(LIGHT)
                .setPadding(8));
        table.addCell(new Cell()
                .add(new Paragraph(value).setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setPadding(8));
    }

    private void addRiskSummary(Document pdf, SummaryReport report, List<Clause> clauses) {
        addSectionHeading(pdf, "Risk Summary");

        Table summary = new Table(UnitValue.createPercentArray(new float[] { 35, 65 }))
                .useAllAvailableWidth()
                .setMarginBottom(16);
        addInfoRow(summary, "Overall Risk", riskLabel(report.getRiskLevel()));
        addInfoRow(summary, "Analysis Status", report.getAnalysisStatus() != null ? report.getAnalysisStatus().name() : "N/A");
        addInfoRow(summary, "Primary Rule", report.getLawRule() != null ? safe(report.getLawRule().getKeyword()) : "N/A");
        pdf.add(summary);

        pdf.add(new Paragraph("Risk Distribution")
                .setFontSize(12)
                .setBold()
                .setMarginTop(6)
                .setMarginBottom(8));
        pdf.add(riskDistributionChart(clauses, report.getRiskLevel()));
    }

    private Table riskDistributionChart(List<Clause> clauses, RiskLevel fallbackRisk) {
        Map<RiskLevel, Integer> counts = new EnumMap<>(RiskLevel.class);
        for (RiskLevel risk : RiskLevel.values()) {
            counts.put(risk, 0);
        }
        if (clauses.isEmpty() && fallbackRisk != null) {
            counts.put(fallbackRisk, 1);
        } else {
            for (Clause clause : clauses) {
                RiskLevel risk = clause.getRiskLevel() != null ? clause.getRiskLevel() : RiskLevel.LOW;
                counts.put(risk, counts.get(risk) + 1);
            }
        }

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        Table chart = new Table(UnitValue.createPercentArray(new float[] { 22, 63, 15 }))
                .useAllAvailableWidth()
                .setMarginBottom(18);

        addChartRow(chart, "High", counts.get(RiskLevel.HIGH), total, HIGH);
        addChartRow(chart, "Medium", counts.get(RiskLevel.MEDIUM), total, MEDIUM);
        addChartRow(chart, "Low", counts.get(RiskLevel.LOW), total, LOW);
        return chart;
    }

    private void addChartRow(Table chart, String label, int count, int total, Color color) {
        chart.addCell(new Cell()
                .add(new Paragraph(label).setFontSize(10).setBold())
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(4));

        float percent = total == 0 ? 0 : (count * 100f / total);
        chart.addCell(new Cell()
                .add(bar(percent, color))
                .setBorder(Border.NO_BORDER)
                .setPadding(4));

        chart.addCell(new Cell()
                .add(new Paragraph(String.valueOf(count)).setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(4));
    }

    private Table bar(float percent, Color color) {
        float filled = Math.max(1f, percent);
        float empty = Math.max(1f, 100f - percent);
        Table bar = new Table(UnitValue.createPercentArray(new float[] { filled, empty }))
                .useAllAvailableWidth();
        bar.addCell(new Cell().setHeight(12).setBorder(Border.NO_BORDER).setBackgroundColor(percent > 0 ? color : LIGHT));
        bar.addCell(new Cell().setHeight(12).setBorder(Border.NO_BORDER).setBackgroundColor(LIGHT));
        return bar;
    }

    private void addAiSection(Document pdf, String title, String text) {
        addSectionHeading(pdf, title);
        pdf.add(new Paragraph(text)
                .setFontSize(10.5f)
                .setMultipliedLeading(1.18f)
                .setMarginBottom(14));
    }

    private void addClauseSection(Document pdf, List<Clause> clauses) {
        addSectionHeading(pdf, "Clause Review");
        if (clauses.isEmpty()) {
            pdf.add(new Paragraph("No clause-level findings are available for this report.")
                    .setFontSize(10)
                    .setFontColor(MUTED));
            return;
        }

        for (Clause clause : clauses) {
            RiskLevel risk = clause.getRiskLevel() != null ? clause.getRiskLevel() : RiskLevel.LOW;
            Color color = riskColor(risk);
            Div block = new Div()
                    .setBorderLeft(new SolidBorder(color, 5))
                    .setBackgroundColor(riskBackground(risk))
                    .setPadding(8)
                    .setMarginBottom(8);

            String heading = "Clause " + id(clause.getClauseId()) + " - " + riskLabel(risk);
            if (clause.getPageNumber() != null) {
                heading += " - page " + clause.getPageNumber();
            }
            block.add(new Paragraph(heading)
                    .setBold()
                    .setFontSize(10)
                    .setFontColor(color)
                    .setMarginBottom(4));
            block.add(new Paragraph(safe(clause.getClauseText()))
                    .setFontSize(9.5f)
                    .setMarginBottom(4));
            if (clause.getRiskReason() != null && !clause.getRiskReason().isBlank()) {
                block.add(new Paragraph("Reason: " + clause.getRiskReason().trim())
                        .setFontSize(9)
                        .setFontColor(MUTED));
            }
            pdf.add(block);
        }
    }

    private void addSectionHeading(Document pdf, String title) {
        pdf.add(new Paragraph(title)
                .setFontSize(15)
                .setBold()
                .setFontColor(BRAND)
                .setMarginTop(8)
                .setMarginBottom(4));
        SolidLine line = new SolidLine(0.6f);
        line.setColor(new DeviceRgb(203, 213, 225));
        pdf.add(new LineSeparator(line).setMarginBottom(10));
    }

    private void addFooter(Document pdf) {
        pdf.add(new Paragraph("This report was generated by ALIS Legal Compliance System.")
                .setFontSize(8)
                .setFontColor(MUTED)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(18));
    }

    private Table riskBadge(RiskLevel riskLevel) {
        Color color = riskColor(riskLevel);
        Table table = new Table(1);
        table.addCell(new Cell()
                .add(new Paragraph("Overall Risk: " + riskLabel(riskLevel))
                        .setBold()
                        .setFontSize(13)
                        .setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(color)
                .setPaddingLeft(24)
                .setPaddingRight(24)
                .setPaddingTop(9)
                .setPaddingBottom(9));
        return table;
    }

    private Color riskColor(RiskLevel risk) {
        if (risk == RiskLevel.HIGH) {
            return HIGH;
        }
        if (risk == RiskLevel.MEDIUM) {
            return MEDIUM;
        }
        return LOW;
    }

    private Color riskBackground(RiskLevel risk) {
        if (risk == RiskLevel.HIGH) {
            return SOFT_HIGH;
        }
        if (risk == RiskLevel.MEDIUM) {
            return SOFT_MEDIUM;
        }
        return SOFT_LOW;
    }

    private String riskLabel(RiskLevel risk) {
        return risk != null ? risk.name() : "UNKNOWN";
    }

    private String lawName(SummaryReport report) {
        if (report.getLawRule() != null
                && report.getLawRule().getAct() != null
                && report.getLawRule().getAct().getActName() != null) {
            return report.getLawRule().getAct().getActName();
        }
        return "South African Legislation";
    }

    private String clientName(Client client) {
        if (client == null) {
            return "N/A";
        }
        String email = client.getEmail() != null ? " (" + client.getEmail() + ")" : "";
        return safe(client.getFullName()) + email;
    }

    private String similarity(BigDecimal score) {
        return score != null ? score.stripTrailingZeros().toPlainString() + "%" : "N/A";
    }

    private String id(Long id) {
        return id != null ? id.toString() : "N/A";
    }

    private String safe(String value) {
        return value != null && !value.isBlank() ? value.trim() : "N/A";
    }

    private String valueOrFallback(String value) {
        return value != null && !value.isBlank() ? value.trim() : "No content provided.";
    }
}
