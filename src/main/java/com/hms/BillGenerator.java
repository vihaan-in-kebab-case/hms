package com.hms;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

public class BillGenerator {

    private static final DeviceRgb NAVY  = new DeviceRgb(15,  23,  42);
    private static final DeviceRgb GOLD  = new DeviceRgb(212, 175, 55);
    private static final DeviceRgb LIGHT = new DeviceRgb(241, 245, 249);
    private static final DeviceRgb GRAY  = new DeviceRgb(100, 116, 139);

    public static File generateBill(Customer customer, String outputDir) throws Exception {

        LocalDateTime checkOut = LocalDateTime.now();
        long nights = ChronoUnit.DAYS.between(customer.getCheckInDateTime(), checkOut);
        if (nights < 1) nights = 1;

        double roomTotal   = nights * customer.getPricePerNight();
        double taxes       = roomTotal * 0.12;
        double grandTotal  = roomTotal + taxes;

        String billNo  = "INV-" + System.currentTimeMillis() % 100000;
        String outPath = outputDir + File.separator + billNo + ".pdf";

        PdfWriter   writer = new PdfWriter(outPath);
        PdfDocument pdf    = new PdfDocument(writer);
        Document    doc    = new Document(pdf);
        doc.setMargins(40, 50, 40, 50);

        PdfFont bold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        Table header = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth()
                .setBackgroundColor(NAVY)
                .setMarginBottom(20);

        Cell hotelName = new Cell()
                .add(new Paragraph("THE GRAND HOTEL")
                        .setFont(bold).setFontSize(24).setFontColor(GOLD)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Excellence in Hospitality")
                        .setFont(regular).setFontSize(10).setFontColor(LIGHT)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setPadding(20);

        header.addCell(hotelName);
        doc.add(header);

        doc.add(new Paragraph("INVOICE / RECEIPT")
                .setFont(bold).setFontSize(14).setFontColor(NAVY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10));

        Table meta = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth().setMarginBottom(20);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        addMetaRow(meta, regular, "Bill No:", billNo, "Check-In:", customer.getCheckInDateTime().format(fmt));
        addMetaRow(meta, regular, "Guest Name:", customer.getName(), "Check-Out:", checkOut.format(fmt));
        addMetaRow(meta, regular, "Contact:", customer.getContact(), "No. of Nights:", String.valueOf(nights));
        doc.add(meta);

        doc.add(new Paragraph("CHARGES BREAKDOWN")
                .setFont(bold).setFontSize(11).setFontColor(NAVY).setMarginBottom(6));

        Table charges = new Table(UnitValue.createPercentArray(new float[]{4, 1, 1, 1}))
                .useAllAvailableWidth().setMarginBottom(20);

        for (String h : new String[]{"Description", "Rate/Night", "Nights", "Amount"}) {
            charges.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(10).setFontColor(LIGHT))
                    .setBackgroundColor(NAVY)
                    .setPadding(8));
        }

        charges.addCell(rowCell(regular, "Room " + customer.getRoomNumber() + " — " + customer.getRoomType()));
        charges.addCell(rowCell(regular, "₹" + String.format("%.2f", customer.getPricePerNight())));
        charges.addCell(rowCell(regular, String.valueOf(nights)));
        charges.addCell(rowCell(regular, "₹" + String.format("%.2f", roomTotal)));

        doc.add(charges);

        Table totals = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .useAllAvailableWidth().setMarginBottom(30);

        addTotalRow(totals, regular, bold, "Sub-Total", "₹" + String.format("%.2f", roomTotal), false);
        addTotalRow(totals, regular, bold, "GST & Taxes (12%)", "₹" + String.format("%.2f", taxes), false);
        addTotalRow(totals, regular, bold, "TOTAL AMOUNT DUE", "₹" + String.format("%.2f", grandTotal), true);

        doc.add(totals);

        doc.add(new Paragraph("Thank you for staying with us. We hope to see you again!")
                .setFont(regular).setFontSize(10).setFontColor(GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("This is a computer-generated invoice and does not require a signature.")
                .setFont(regular).setFontSize(8).setFontColor(GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        doc.close();

        return new File(outPath);
    }

    private static void addMetaRow(Table t, PdfFont font,
                                   String k1, String v1, String k2, String v2) {
        t.addCell(new Cell().add(new Paragraph(k1).setFont(font).setFontSize(9)
                        .setFontColor(new DeviceRgb(100,116,139)))
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(
                        new DeviceRgb(226,232,240), 0.5f))
                .setPaddingBottom(4));
        t.addCell(new Cell().add(new Paragraph(k2).setFont(font).setFontSize(9)
                        .setFontColor(new DeviceRgb(100,116,139)))
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(
                        new DeviceRgb(226,232,240), 0.5f))
                .setPaddingBottom(4));
        t.addCell(new Cell().add(new Paragraph(v1).setFont(font).setFontSize(10))
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(
                        new DeviceRgb(226,232,240), 0.5f))
                .setPaddingBottom(8));
        t.addCell(new Cell().add(new Paragraph(v2).setFont(font).setFontSize(10))
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(
                        new DeviceRgb(226,232,240), 0.5f))
                .setPaddingBottom(8));
    }

    private static Cell rowCell(PdfFont font, String text) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(10)).setPadding(7);
    }

    private static void addTotalRow(Table t, PdfFont regular, PdfFont bold,
                                    String label, String value, boolean highlight) {
        DeviceRgb bg = highlight ? NAVY : new DeviceRgb(248, 250, 252);
        DeviceRgb fg = highlight ? LIGHT : NAVY;
        DeviceRgb vg = highlight ? GOLD  : NAVY;
        int size     = highlight ? 12 : 10;

        t.addCell(new Cell().add(new Paragraph(label)
                        .setFont(highlight ? bold : regular).setFontSize(size).setFontColor(fg))
                .setBackgroundColor(bg).setPadding(8));
        t.addCell(new Cell().add(new Paragraph(value)
                        .setFont(bold).setFontSize(size).setFontColor(vg)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(bg).setPadding(8));
    }
}
