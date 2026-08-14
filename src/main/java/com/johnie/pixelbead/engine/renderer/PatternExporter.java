package com.johnie.pixelbead.engine.renderer;

import com.johnie.pixelbead.engine.model.PatternProject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.w3c.dom.Element;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Exports a pattern sheet as PNG, PDF or a color-code text matrix.
 * <p>
 * PNG and PDF render at 1:1 physical scale: the cell size is derived from
 * the bead diameter (5.0mm or 2.6mm) at the target DPI, so printed sheets
 * match real pegboards. Text export writes the grid as color codes with
 * '.' for empty cells.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
public final class PatternExporter {

    /**
     * Render DPI for raster output; 1:1 means cellSize = beadSize at this DPI.
     */
    private static final int DPI = 300;

    private PatternExporter() {
    }

    public static void writePng(PatternProject project, Path target) throws IOException {
        BufferedImage img = renderAtScale(project);
        writePngWithDpi(img, target, DPI);
    }

    public static void writePdf(PatternProject project, Path target) throws IOException {
        BufferedImage img = renderAtScale(project);
        ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
        ImageIO.write(img, "png", pngBytes);

        // Image pixels at 300dpi -> PDF points at 72dpi keeps physical size.
        float ptW = img.getWidth() * 72f / DPI;
        float ptH = img.getHeight() * 72f / DPI;

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(ptW, ptH));
            doc.addPage(page);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, pngBytes.toByteArray(), "pattern");
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(pdImage, 0, 0, ptW, ptH);
            }
            doc.save(target.toFile());
        }
    }

    /**
     * Writes a multi-board PDF: an overview page followed by one page per
     * sub-grid tile (the grid is split every {@code tileSize} rows/columns).
     * Every page is A4 with the sheet scaled to fill the usable area plus a
     * header naming the board and its row/column range, so the printed pages
     * can be aligned onto real pegboards.
     *
     * @param tileSize sub-grid interval; values below 2 fall back to a single
     *                 overview page
     */
    public static void writeTiledPdf(PatternProject project, int tileSize, Path target) throws IOException {
        if (tileSize < 2) {
            writePdf(project, target);
            return;
        }
        int columns = project.board().columns();
        int rows = project.board().rows();
        int tileCols = (columns + tileSize - 1) / tileSize;
        int tileRows = (rows + tileSize - 1) / tileSize;
        int total = tileCols * tileRows;
        int cell = (int) Math.round(project.board().beadSizeMm() / 25.4 * DPI);

        BufferedImage img = renderAtScale(project);
        try (PDDocument doc = new PDDocument()) {
            // Overview page first, then one page per tile top-left to bottom-right.
            addTiledPage(doc, img, "Overview  " + total + " boards", false);
            int n = 1;
            for (int ty = 0; ty < tileRows; ty++) {
                for (int tx = 0; tx < tileCols; tx++) {
                    int x0 = tx * tileSize;
                    int y0 = ty * tileSize;
                    int w = Math.min(tileSize, columns - x0);
                    int h = Math.min(tileSize, rows - y0);
                    BufferedImage tile = img.getSubimage(x0 * cell, y0 * cell, w * cell, h * cell);
                    String header = String.format("Board %d/%d   Rows %d-%d   Cols %d-%d",
                            n, total, y0 + 1, y0 + h, x0 + 1, x0 + w);
                    addTiledPage(doc, tile, header, true);
                    n++;
                }
            }
            doc.save(target.toFile());
        }
    }

    /**
     * A4 page with the sheet scaled to fill the usable area, centred, with an
     * optional header line and a board outline.
     */
    private static void addTiledPage(PDDocument doc, BufferedImage img, String header, boolean outline)
            throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        float pw = PDRectangle.A4.getWidth();
        float ph = PDRectangle.A4.getHeight();
        // ~12mm
        float margin = 34f;
        // header band
        float headerH = 42f;
        float availW = pw - 2 * margin;
        float availH = ph - 2 * margin - headerH;
        float scale = Math.min(availW / img.getWidth(), availH / img.getHeight());
        float drawW = img.getWidth() * scale;
        float drawH = img.getHeight() * scale;
        float x = margin + (availW - drawW) / 2f;
        float y = margin + headerH + (availH - drawH) / 2f;

        ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
        ImageIO.write(img, "png", pngBytes);
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, pngBytes.toByteArray(), "tile");
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            if (header != null) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.setNonStrokingColor(0.3f, 0.3f, 0.35f);
                cs.newLineAtOffset(margin, ph - margin - 16);
                cs.showText(header);
                cs.endText();
            }
            cs.drawImage(pdImage, x, y, drawW, drawH);
            if (outline) {
                cs.setStrokingColor(0.45f, 0.45f, 0.5f);
                cs.setLineWidth(1.2f);
                cs.addRect(x, y, drawW, drawH);
                cs.stroke();
            }
        }
    }

    public static void writeText(PatternProject project, Path target) throws IOException {
        int[][] grid = project.grid();
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(target, StandardCharsets.UTF_8))) {
            for (int[] row : grid) {
                for (int x = 0; x < row.length; x++) {
                    if (x > 0) {
                        writer.print(' ');
                    }
                    writer.print(row[x] >= 0 ? project.palette().colorAt(row[x]).code() : ".");
                }
                writer.println();
            }
        }
    }

    private static BufferedImage renderAtScale(PatternProject project) {
        double mm = project.board().beadSizeMm();
        int cellSize = (int) Math.round(mm / 25.4 * DPI);
        return PatternRenderer.render(project, cellSize);
    }

    /**
     * Writes a PNG with the physical resolution (DPI) recorded in its pHYs chunk.
     */
    private static void writePngWithDpi(BufferedImage img, Path target, int dpi) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(target.toFile())) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            IIOMetadata meta = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(img), param);
            Element tree = (Element) meta.getAsTree("javax_imageio_png_1.0");
            Element phys = (Element) tree.getElementsByTagName("pHYs").item(0);
            if (phys == null) {
                phys = new IIOMetadataNode("pHYs");
                tree.appendChild(phys);
            }
            double pixelsPerMeter = dpi / 0.0254;
            phys.setAttribute("pixelsPerUnitXAxis", String.valueOf(Math.round(pixelsPerMeter)));
            phys.setAttribute("pixelsPerUnitYAxis", String.valueOf(Math.round(pixelsPerMeter)));
            phys.setAttribute("unitSpecifier", "meter");
            meta.setFromTree("javax_imageio_png_1.0", tree);
            writer.write(null, new IIOImage(img, null, meta), param);
        } finally {
            writer.dispose();
        }
    }
}
