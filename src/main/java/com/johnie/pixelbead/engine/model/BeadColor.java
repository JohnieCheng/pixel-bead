package com.johnie.pixelbead.engine.model;

import com.johnie.pixelbead.engine.quantizer.ColorSpace;

/**
 * A single bead color entry from a brand palette.
 * <p>
 * The CIELAB representation is computed once at construction and cached,
 * so color matching never pays the RGB-LAB conversion cost in a hot loop.
 */
public final class BeadColor {

    private final String code;
    private final String name;
    private final int r;
    private final int g;
    private final int b;
    private final double[] lab;
    /**
     * Official hex value (#RRGGBB);
     */
    private String hex;

    public BeadColor(String code, String name, int r, int g, int b) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Color code must not be blank");
        }
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            throw new IllegalArgumentException("RGB channels must be in [0, 255]: " + r + "," + g + "," + b);
        }
        this.code = code;
        this.name = name == null ? "" : name;
        this.r = r;
        this.g = g;
        this.b = b;
        this.lab = ColorSpace.rgbToLab(r, g, b);
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public int r() {
        return r;
    }

    public int g() {
        return g;
    }

    public int b() {
        return b;
    }

    public String hex() {
        return hex;
    }

    /**
     * Sets the official hex value; called by the palette parser.
     */
    public void setHex(String hex) {
        this.hex = hex == null ? "" : hex;
    }

    /**
     * Cached CIELAB (D65) values: {L, a, b}. Must not be modified.
     */
    public double[] lab() {
        return lab;
    }

    @Override
    public String toString() {
        return code + (name.isEmpty() ? "" : " " + name) + " #" + Integer.toHexString((r << 16) | (g << 8) | b);
    }
}
