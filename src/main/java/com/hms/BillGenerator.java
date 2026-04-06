package com.hms;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.*;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.itextpdf.io.font.constants.StandardFonts;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BillGenerator {

    private static final DeviceRgb NAVY  = new DeviceRgb(15,  23,  42);
    private static final DeviceRgb GOLD  = new DeviceRgb(212, 175, 55);
    private static final DeviceRgb LIGHT = new DeviceRgb(241, 245, 249);
    private static final DeviceRgb GRAY  = new DeviceRgb(100, 116, 139);

    // Amenity prices (per stay)
    public static final double PRICE_BREAKFAST = 350.0;
    public static final double PRICE_WIFI      = 199.0;
    public static final double PRICE_CAB       = 750.0;

    /**
     * @param customer   booking being checked out
     * @param nights     number of nights (entered manually by staff)
     * @param discount   discount percentage (0–100)
     * @param amenities  list of amenity names selected (e.g. "Breakfast", "Wi-Fi", "Cab Service")
     * @param outputDir  folder to save the PDF
     */
    public static File generateBill(Customer customer, int nights, double discount,
                                    List<String> amenities, String outputDir) throws Exception {

        double roomSubtotal = nights * customer.getPricePerNight();
        double discountAmt  = roomSubtotal * (discount / 100.0);
        double roomTotal    = roomSubtotal - discountAmt;

        // Amenity charges
        double amenityTotal = 0;
        for (String a : amenities) {
            if (a.equals("Breakfast"))    amenityTotal += PRICE_BREAKFAST * nights;
            else if (a.equals("Wi-Fi"))   amenityTotal += PRICE_WIFI;
            else if (a.equals("Cab Service")) amenityTotal += PRICE_CAB;
        }

        double subtotal   = roomTotal + amenityTotal;
        double taxes      = subtotal * 0.12;
        double grandTotal = subtotal + taxes;

        String billNo  = "INV-" + System.currentTimeMillis() % 100000;
        String outPath = outputDir + File.separator + billNo + ".pdf";

        PdfWriter   writer = new PdfWriter(outPath);
        PdfDocument pdf    = new PdfDocument(writer);
        Document    doc    = new Document(pdf);
        doc.setMargins(40, 50, 40, 50);

        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // ── Header ───────────────────────────────────────────────────
        Table header = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth().setBackgroundColor(NAVY).setMarginBottom(20);
        header.addCell(new Cell()
                .add(new Paragraph("THE GRAND HOTEL")
                        .setFont(bold).setFontSize(24).setFontColor(GOLD)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Excellence in Hospitality")
                        .setFont(regular).setFontSize(10).setFontColor(LIGHT)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setPadding(20));
        doc.add(header);

        doc.add(new Paragraph("INVOICE / RECEIPT")
                .setFont(bold).setFontSize(14).setFontColor(NAVY)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

        // ── Meta ─────────────────────────────────────────────────────
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        Table meta = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth().setMarginBottom(20);
        addMetaRow(meta, regular, "Bill No:",      billNo,
                                  "Check-In:",     customer.getCheckInDateTime().format(fmt));
        addMetaRow(meta, regular, "Guest Name:",   customer.getName(),
                                  "Check-Out:",    LocalDateTime.now().format(fmt));
        addMetaRow(meta, regular, "Contact:",      customer.getContact(),
                                  "No. of Nights:",String.valueOf(nights));
        doc.add(meta);

        // ── Charges table ─────────────────────────────────────────────
        doc.add(new Paragraph("CHARGES BREAKDOWN")
                .setFont(bold).setFontSize(11).setFontColor(NAVY).setMarginBottom(6));

        Table charges = new Table(UnitValue.createPercentArray(new float[]{4, 1, 1, 1}))
                .useAllAvailableWidth().setMarginBottom(10);
        for (String h : new String[]{"Description", "Rate", "Qty", "Amount"}) {
            charges.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(10).setFontColor(LIGHT))
                    .setBackgroundColor(NAVY).setPadding(8));
        }

        // Room row
        charges.addCell(rowCell(regular, "Room " + customer.getRoomNumber() + " — " + customer.getRoomType()));
        charges.addCell(rowCell(regular, "₹" + String.format("%.0f", customer.getPricePerNight())));
        charges.addCell(rowCell(regular, nights + " nights"));
        charges.addCell(rowCell(regular, "₹" + String.format("%.2f", roomSubtotal)));

        // Discount row (if any)
        if (discount > 0) {
            charges.addCell(rowCell(regular, "Discount (" + String.format("%.0f", discount) + "%)"));
            charges.addCell(rowCell(regular, ""));
            charges.addCell(rowCell(regular, ""));
            charges.addCell(rowCell(regular, "-₹" + String.format("%.2f", discountAmt)));
        }

        // Amenity rows
        for (String a : amenities) {
            double price = 0;
            String qtyLabel = "";
            if (a.equals("Breakfast"))      { price = PRICE_BREAKFAST; qtyLabel = nights + " days"; }
            else if (a.equals("Wi-Fi"))     { price = PRICE_WIFI;      qtyLabel = "1 (flat)"; }
            else if (a.equals("Cab Service")){ price = PRICE_CAB;      qtyLabel = "1 trip"; }
            double lineTotal = a.equals("Breakfast") ? price * nights : price;
            charges.addCell(rowCell(regular, a));
            charges.addCell(rowCell(regular, "₹" + String.format("%.0f", price)));
            charges.addCell(rowCell(regular, qtyLabel));
            charges.addCell(rowCell(regular, "₹" + String.format("%.2f", lineTotal)));
        }

        doc.add(charges);

        // ── Totals ────────────────────────────────────────────────────
        Table totals = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .useAllAvailableWidth().setMarginBottom(30);
        addTotalRow(totals, regular, bold, "Sub-Total",          "₹" + String.format("%.2f", subtotal),   false);
        addTotalRow(totals, regular, bold, "GST & Taxes (12%)",  "₹" + String.format("%.2f", taxes),      false);
        addTotalRow(totals, regular, bold, "TOTAL AMOUNT DUE",   "₹" + String.format("%.2f", grandTotal), true);
        doc.add(totals);

        // ── Footer ────────────────────────────────────────────────────
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
        com.itextpdf.layout.borders.Border sep =
                new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(226,232,240), 0.5f);
        t.addCell(new Cell().add(new Paragraph(k1).setFont(font).setFontSize(9)
                        .setFontColor(GRAY)).setBorderBottom(sep).setPaddingBottom(4));
        t.addCell(new Cell().add(new Paragraph(k2).setFont(font).setFontSize(9)
                        .setFontColor(GRAY)).setBorderBottom(sep).setPaddingBottom(4));
        t.addCell(new Cell().add(new Paragraph(v1).setFont(font).setFontSize(10))
                        .setBorderBottom(sep).setPaddingBottom(8));
        t.addCell(new Cell().add(new Paragraph(v2).setFont(font).setFontSize(10))
                        .setBorderBottom(sep).setPaddingBottom(8));
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
