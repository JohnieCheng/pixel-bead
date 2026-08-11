package com.johnie.pixelbead.engine.renderer;

import com.johnie.pixelbead.engine.model.PatternProject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
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
