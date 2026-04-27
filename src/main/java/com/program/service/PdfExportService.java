package com.program.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.program.entity.Transactions;
import com.program.entity.User;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfExportService {

    private static final Color DARK_TEAL  = new Color(22, 49, 60);
    private static final Color ACCENT_RED = new Color(164, 57, 49);
    private static final Color LIGHT_GREY = new Color(244, 247, 251);
    private static final Color MID_GREY   = new Color(72, 101, 129);
    private static final Color GREEN      = new Color(6, 118, 71);
    private static final Color RED_TEXT   = new Color(180, 35, 24);

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public void exportTransactions(User user,
                                   List<Transactions> transactions,
                                   OutputStream out) throws Exception {

        Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter writer = PdfWriter.getInstance(doc, out);

        // Header/footer on every page
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter w, Document d) {
                PdfContentByte cb = w.getDirectContent();

                // Top bar
                cb.setColorFill(DARK_TEAL);
                cb.rectangle(0, PageSize.A4.getHeight() - 40, PageSize.A4.getWidth(), 40);
                cb.fill();

                // Bottom bar
                cb.setColorFill(DARK_TEAL);
                cb.rectangle(0, 0, PageSize.A4.getWidth(), 28);
                cb.fill();

                // Footer text
                Font footerFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.WHITE);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase("Secure Bank  ·  Confidential  ·  Page " + w.getPageNumber(), footerFont),
                        PageSize.A4.getWidth() / 2, 10, 0);
            }
        });

        doc.open();

        // ── Bank name in header ─────────────────────────────
        Font bankFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE);
        PdfContentByte canvas = writer.getDirectContent();
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                new Phrase("Secure Bank", bankFont), 36, PageSize.A4.getHeight() - 26, 0);

        Font headerRight = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.WHITE);
        ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                new Phrase("Transaction Statement", headerRight),
                PageSize.A4.getWidth() - 36, PageSize.A4.getHeight() - 26, 0);

        // ── Account summary block ───────────────────────────
        doc.add(Chunk.NEWLINE);

        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(100);
        summary.setSpacingBefore(16);
        summary.setSpacingAfter(20);
        summary.setWidths(new float[]{1f, 1f});

        addSummaryCell(summary, "Account Holder", user.getName());
        addSummaryCell(summary, "Username",        user.getUsername());
        addSummaryCell(summary, "Email",           user.getEmail());
        addSummaryCell(summary, "Current Balance", "Rs " + String.format("%.2f", user.getBalance()));
        addSummaryCell(summary, "Total Records",   transactions.size() + " transactions");
        addSummaryCell(summary, "Generated On",
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));

        doc.add(summary);

        // ── Section title ───────────────────────────────────
        Font sectionFont = new Font(Font.HELVETICA, 11, Font.BOLD, DARK_TEAL);
        Paragraph sectionTitle = new Paragraph("Transaction History", sectionFont);
        sectionTitle.setSpacingAfter(8);
        doc.add(sectionTitle);

        // ── Transactions table ──────────────────────────────
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1f, 2f, 2.5f, 1.5f});

        // Header row
        String[] headers = {"Type", "Amount", "Reference", "Details", "Date"};
        Font hFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hFont));
            cell.setBackgroundColor(DARK_TEAL);
            cell.setPadding(8);
            cell.setBorderColor(DARK_TEAL);
            table.addCell(cell);
        }

        // Data rows
        boolean odd = true;
        Font cellFont  = new Font(Font.HELVETICA, 8, Font.NORMAL, DARK_TEAL);
        Font amtCredit = new Font(Font.HELVETICA, 8, Font.BOLD,   GREEN);
        Font amtDebit  = new Font(Font.HELVETICA, 8, Font.BOLD,   RED_TEXT);

        for (Transactions tx : transactions) {
            Color bg = odd ? Color.WHITE : LIGHT_GREY;
            odd = !odd;

            boolean isCredit = "CREDIT".equals(tx.getType()) || "TRANSFER_IN".equals(tx.getType());
            Font amtFont = isCredit ? amtCredit : amtDebit;
            String amtPrefix = isCredit ? "+ Rs " : "- Rs ";

            addCell(table, tx.getType(),                                         cellFont, bg);
            addCell(table, amtPrefix + String.format("%.2f", tx.getAmount()),    amtFont,  bg);
            addCell(table, tx.getReferenceNumber() != null ? tx.getReferenceNumber() : "-", cellFont, bg);
            addCell(table, tx.getDetails()         != null ? tx.getDetails()     : "-",     cellFont, bg);
            addCell(table, tx.getDate()            != null ? tx.getDate().format(FMT) : "-", cellFont, bg);
        }

        doc.add(table);

        // ── Closing note ────────────────────────────────────
        Font noteFont = new Font(Font.HELVETICA, 8, Font.ITALIC, MID_GREY);
        Paragraph note = new Paragraph(
                "\nThis is a system-generated statement and does not require a signature.", noteFont);
        note.setSpacingBefore(16);
        note.setAlignment(Element.ALIGN_CENTER);
        doc.add(note);

        doc.close();
    }

    private void addSummaryCell(PdfPTable table, String label, String value) {
        Font labelFont = new Font(Font.HELVETICA, 8, Font.NORMAL, MID_GREY);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD, DARK_TEAL);

        PdfPCell cell = new PdfPCell();
        cell.addElement(new Phrase(label, labelFont));
        cell.addElement(new Phrase(value, valueFont));
        cell.setPadding(10);
        cell.setBorderColor(new Color(220, 228, 235));
        cell.setBackgroundColor(LIGHT_GREY);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBackgroundColor(bg);
        cell.setBorderColor(new Color(220, 228, 235));
        table.addCell(cell);
    }
}