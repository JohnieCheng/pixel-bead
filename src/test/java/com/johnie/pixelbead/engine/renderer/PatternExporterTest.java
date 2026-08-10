package com.johnie.pixelbead.engine.renderer;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        grid[1][1] = 5;
        grid[0][2] = 0;
        grid[2][0] = 12;
        project = new PatternProject(board, palette, grid);
    }

    @Test
    void textExportMatchesGridShape(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("pattern.txt");
        PatternExporter.writeText(project, file);

        assertEquals(3, Files.readAllLines(file).size());
        String line = Files.readAllLines(file).get(0);
        // Row 0: empty, empty, palette index 0 -> ". . CE001"
        assertEquals(". . " + project.palette().colorAt(0).code(), line);
        // Row 1: empty, index 5, empty
        assertEquals(". " + project.palette().colorAt(5).code() + " .", Files.readAllLines(file).get(1));
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
}
