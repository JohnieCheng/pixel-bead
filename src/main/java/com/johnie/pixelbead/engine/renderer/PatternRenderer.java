package com.johnie.pixelbead.engine.renderer;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadColor;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Offline pattern sheet renderer (pure AWT, no JavaFX).
 * <p>
 * Produces a printable 1:1 sheet: bead cells with color-code labels, fine
 * grid lines, bold sub-grid lines, row/column coordinates and a bead count
 * legend. Cell size is expressed in pixels and chosen by the exporter
 * according to the target DPI.
 *
 * @author johnie
 * @version 3.0.0
 * @since 2026/08/10
 */
public final class PatternRenderer {

    private static final int AXIS_MARGIN = 36;
    private static final int EDGE_MARGIN = 12;
    private static final int LEGEND_ROW_HEIGHT = 18;
    private static final int LEGEND_TOP_PAD = 28;
    private static final int LEGEND_SWATCH = 12;
    private static final int LEGEND_GAP = 12;
    private static final int LEGEND_PER_LINE = 4;

    private static final Color GRID_LINE = new Color(0xD6D6D6);
    private static final Color SUB_GRID_LINE = new Color(0x9E9E9E);
    private static final Color BORDER = new Color(0x5A5A5A);
    private static final Color AXIS_TEXT = new Color(0x444444);
    private static final Color SWATCH_BORDER = new Color(0x999999);

    private PatternRenderer() {
    }

    /**
     * Renders the full pattern sheet.
     *
     * @param project  pattern to render
     * @param cellSize cell size in pixels
     * @return rendered sheet image (RGB)
     */
    public static BufferedImage render(PatternProject project, int cellSize) {
        BeadBoard board = project.board();
        int cols = board.columns();
        int rows = board.rows();

        int[] counts = project.colorCounts();
        int usedColors = 0;
        for (int count : counts) {
            if (count > 0) {
                usedColors++;
            }
        }
        int legendLines = (usedColors + LEGEND_PER_LINE - 1) / LEGEND_PER_LINE;
        int legendHeight = usedColors > 0 ? LEGEND_TOP_PAD + legendLines * LEGEND_ROW_HEIGHT + 10 : 0;

        int gridW = cols * cellSize;
        int gridH = rows * cellSize;
        int imgW = AXIS_MARGIN + gridW + EDGE_MARGIN;
        int imgH = AXIS_MARGIN + gridH + legendHeight;

        BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imgW, imgH);

            drawGrid(g, project, cellSize);
            drawAxis(g, cols, rows, cellSize);
            drawLegend(g, project, counts, imgW, cellSize);
        } finally {
            g.dispose();
        }
        return img;
    }

    private static void drawGrid(Graphics2D g, PatternProject project, int cellSize) {
        BeadBoard board = project.board();
        int cols = board.columns();
        int rows = board.rows();
        int[][] grid = project.grid();

        // Bead cells with color-code labels.
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int idx = grid[y][x];
                if (idx < 0) {
                    continue;
                }
                BeadColor c = project.palette().colorAt(idx);
                int px = AXIS_MARGIN + x * cellSize;
                int py = AXIS_MARGIN + y * cellSize;
                g.setColor(new Color(c.r(), c.g(), c.b()));
                g.fillRect(px, py, cellSize, cellSize);
                if (cellSize >= 14) {
                    drawCodeLabel(g, c, px, py, cellSize);
                }
            }
        }

        // Fine grid lines.
        g.setColor(GRID_LINE);
        g.setStroke(new BasicStroke(1f));
        for (int x = 0; x <= cols; x++) {
            int px = AXIS_MARGIN + x * cellSize;
            g.drawLine(px, AXIS_MARGIN, px, AXIS_MARGIN + rows * cellSize);
        }
        for (int y = 0; y <= rows; y++) {
            int py = AXIS_MARGIN + y * cellSize;
            g.drawLine(AXIS_MARGIN, py, AXIS_MARGIN + cols * cellSize, py);
        }

        // Bold sub-grid lines.
        int interval = board.subGridInterval();
        g.setColor(SUB_GRID_LINE);
        g.setStroke(new BasicStroke(2f));
        for (int x = 0; x <= cols; x += interval) {
            int px = AXIS_MARGIN + x * cellSize;
            g.drawLine(px, AXIS_MARGIN, px, AXIS_MARGIN + rows * cellSize);
        }
        for (int y = 0; y <= rows; y += interval) {
            int py = AXIS_MARGIN + y * cellSize;
            g.drawLine(AXIS_MARGIN, py, AXIS_MARGIN + cols * cellSize, py);
        }

        // Outer border.
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(AXIS_MARGIN, AXIS_MARGIN, cols * cellSize, rows * cellSize);
    }

    private static void drawCodeLabel(Graphics2D g, BeadColor c, int px, int py, int cellSize) {
        String code = c.code();
        double luminance = 0.299 * c.r() + 0.587 * c.g() + 0.114 * c.b();
        g.setColor(luminance > 140 ? Color.BLACK : Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(8, cellSize / 3)));
        FontMetrics fm = g.getFontMetrics();
        int maxWidth = cellSize - 2;
        while (fm.stringWidth(code) > maxWidth && code.length() > 1) {
            code = code.substring(0, code.length() - 1);
        }
        int tw = fm.stringWidth(code);
        int th = fm.getAscent();
        g.drawString(code, px + (cellSize - tw) / 2, py + (cellSize + th) / 2);
    }

    private static void drawAxis(Graphics2D g, int cols, int rows, int cellSize) {
        g.setColor(AXIS_TEXT);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        for (int x = 0; x < cols; x++) {
            String s = String.valueOf(x);
            int tw = fm.stringWidth(s);
            g.drawString(s, AXIS_MARGIN + x * cellSize + (cellSize - tw) / 2, 24);
        }
        for (int y = 0; y < rows; y++) {
            String s = String.valueOf(y);
            int tw = fm.stringWidth(s);
            g.drawString(s, AXIS_MARGIN - tw - 6, AXIS_MARGIN + y * cellSize + (cellSize + fm.getAscent()) / 2);
        }
    }

    private static void drawLegend(Graphics2D g, PatternProject project, int[] counts, int imgW, int cellSize) {
        BeadPalette palette = project.palette();
        g.setColor(AXIS_TEXT);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        // Header baseline sits clear of the grid bottom by the font ascent
        // (the text top must not touch the last grid row).
        int legendY = AXIS_MARGIN + project.board().rows() * cellSize + 24;
        g.drawString("Bead Count", AXIS_MARGIN, legendY - 8);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();

        int x = AXIS_MARGIN;
        // Leave room below the "Bead Count" header so the first legend row's
        // swatches never overlap the header text (dark swatches used to cover it).
        int y = legendY + 16;
        int perLine = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] == 0) {
                continue;
            }
            BeadColor c = palette.colorAt(i);
            g.setColor(new Color(c.r(), c.g(), c.b()));
            g.fillRect(x, y - LEGEND_SWATCH, LEGEND_SWATCH, LEGEND_SWATCH);
            g.setColor(SWATCH_BORDER);
            g.drawRect(x, y - LEGEND_SWATCH, LEGEND_SWATCH, LEGEND_SWATCH);
            g.setColor(Color.BLACK);
            String entry = c.code() + " x" + counts[i];
            g.drawString(entry, x + LEGEND_SWATCH + 4, y);
            x += LEGEND_SWATCH + 4 + fm.stringWidth(entry) + LEGEND_GAP;
            if (++perLine >= LEGEND_PER_LINE) {
                perLine = 0;
                x = AXIS_MARGIN;
                y += LEGEND_ROW_HEIGHT;
            }
        }
    }
}
