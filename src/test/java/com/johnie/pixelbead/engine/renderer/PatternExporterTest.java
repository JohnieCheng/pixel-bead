package com.johnie.pixelbead.engine.renderer;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests PNG/PDF/Text pattern export.
 *
 * @author johnie
 * @version 3.0.0
 * @since 2026/08/10
 */
class PatternExporterTest {

    private static PatternProject project;

    @BeforeAll
    static void setUp() throws IOException {
        BeadPalette palette = BeadPalette.loadResource("/palettes/mard_standard.json");
        BeadBoard board = new BeadBoard(3, 3, 5.0, 1);
        int[][] grid = new int[3][3];
        for (int[] row : grid) {
            Arrays.fill(row, -1);
        }
        grid[1][1] = 9;   // A10
        grid[0][2] = 0;   // A1
        grid[2][0] = 1;   // A2
        project = new PatternProject(board, palette, grid);
    }

    @Test
    void pngExportProducesReadableImage(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("pattern.png");
        PatternExporter.writePng(project, file);

        assertTrue(Files.size(file) > 0);
        BufferedImage img = ImageIO.read(file.toFile());
        // 5mm bead @300dpi -> cellSize = round(5/25.4*300) = 59
        int cellSize = 59;
        assertEquals(36 + 3 * cellSize + 12, img.getWidth());
    }

    @Test
    void pdfExportProducesSinglePageAtPhysicalSize(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("pattern.pdf");
        PatternExporter.writePdf(project, file);

        assertTrue(Files.size(file) > 0);
        try (PDDocument doc = Loader.loadPDF(file.toFile())) {
            assertEquals(1, doc.getNumberOfPages());
            float widthPt = doc.getPage(0).getMediaBox().getWidth();
            // image width px at 300dpi -> pt at 72dpi
            int cellSize = 59;
            int imgW = 36 + 3 * cellSize + 12;
            assertEquals(imgW * 72f / 300, widthPt, 0.5);
        }
    }

    @Test
    void tiledPdfAddsOnePagePerSubGridTile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("tiled.pdf");
        // 3x3 board with sub-grid 2 -> 2x2 tiles + overview = 5 pages.
        PatternExporter.writeTiledPdf(project, 2, file);

        try (PDDocument doc = Loader.loadPDF(file.toFile())) {
            assertEquals(5, doc.getNumberOfPages());
            // Every page is A4 (595 x 842 pt).
            for (PDPage page : doc.getPages()) {
                assertEquals(595.0f, page.getMediaBox().getWidth(), 0.5);
                assertEquals(842.0f, page.getMediaBox().getHeight(), 0.5);
            }
        }
    }

    @Test
    void tiledPdfFallsBackToSinglePageBelowIntervalTwo(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("flat.pdf");
        PatternExporter.writeTiledPdf(project, 1, file);

        try (PDDocument doc = Loader.loadPDF(file.toFile())) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    void csvExportListsUsedColoursNaturallySorted(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("list.csv");
        PatternExporter.writeCsv(project, file);

        List<String> lines = Files.readAllLines(file);
        assertEquals("code,hex,count", lines.get(0).replace("\uFEFF", ""));
        // Header + one row per used colour.
        assertEquals(4, lines.size());
        // Natural order: row 2 is A2 (not A10), proving numeric sort.
        assertTrue(lines.get(1).startsWith("A1,"));
        assertTrue(lines.get(2).startsWith("A2,"));
        assertTrue(lines.get(3).startsWith("A10,"));
        for (int i = 1; i < lines.size(); i++) {
            assertTrue(lines.get(i).matches("[A-Z]+\\d+,#[0-9A-F]{6},\\d+"));
        }
    }
}
