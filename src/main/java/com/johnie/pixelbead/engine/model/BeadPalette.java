package com.johnie.pixelbead.engine.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnie.pixelbead.engine.quantizer.ColorDifference;
import com.johnie.pixelbead.engine.quantizer.ColorSpace;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A bead color palette loaded from a JSON resource file.
 * <p>
 * JSON schema:
 * <pre>
 * {
 * "brand": "Mard",
 * "colors": [
 * { "code": "CE001", "name": "", "rgb": [60, 85, 93] }
 * ]
 * }
 * </pre>
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
public final class BeadPalette {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String brand;
    private final List<BeadColor> colors;

    private BeadPalette(String brand, List<BeadColor> colors) {
        this.brand = brand;
        this.colors = colors;
    }

    /**
     * Loads a palette from a JSON file.
     *
     * @param jsonPath path to the palette JSON file
     * @return loaded palette
     * @throws IOException if the file cannot be read or parsed
     */
    public static BeadPalette load(Path jsonPath) throws IOException {
        try (InputStream in = Files.newInputStream(jsonPath)) {
            return parse(in);
        }
    }

    /**
     * Loads a palette from a classpath resource.
     *
     * @param resourcePath classpath resource path, e.g. "/palettes/mard_standard.json"
     * @return loaded palette
     * @throws IOException if the resource cannot be read or parsed
     */
    public static BeadPalette loadResource(String resourcePath) throws IOException {
        try (InputStream in = BeadPalette.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Palette resource not found: " + resourcePath);
            }
            return parse(in);
        }
    }

    private static BeadPalette parse(InputStream in) throws IOException {
        JsonNode root = MAPPER.readTree(in);
        String brand = root.path("brand").asText("unknown");
        JsonNode colorsNode = root.path("colors");
        List<BeadColor> colors = new ArrayList<>(colorsNode.size());
        for (JsonNode node : colorsNode) {
            JsonNode rgb = node.path("rgb");
            BeadColor color = new BeadColor(
                    node.path("code").asText(),
                    node.path("name").asText(),
                    rgb.get(0).asInt(),
                    rgb.get(1).asInt(),
                    rgb.get(2).asInt());
            color.setHex(node.path("hex").asText(""));
            colors.add(color);
        }
        if (colors.isEmpty()) {
            throw new IOException("Palette contains no colors");
        }
        return new BeadPalette(brand, List.copyOf(colors));
    }

    public String brand() {
        return brand;
    }

    public int size() {
        return colors.size();
    }

    public BeadColor colorAt(int index) {
        return colors.get(index);
    }

    /**
     * Finds the palette index of the perceptually closest color using CIEDE2000.
     *
     * @param r red channel 0-255
     * @param g green channel 0-255
     * @param b blue channel 0-255
     * @return palette index of the best match
     */
    public int nearestIndex(int r, int g, int b) {
        double[] target = ColorSpace.rgbToLab(r, g, b);
        int best = 0;
        double bestDelta = Double.MAX_VALUE;
        for (int i = 0; i < colors.size(); i++) {
            double delta = ColorDifference.de2000(target, colors.get(i).lab());
            if (delta < bestDelta) {
                bestDelta = delta;
                best = i;
            }
        }
        return best;
    }

    /**
     * Finds the perceptually closest {@link BeadColor} using CIEDE2000.
     */
    public BeadColor nearestColor(int r, int g, int b) {
        return colors.get(nearestIndex(r, g, b));
    }

    public List<BeadColor> colors() {
        return colors;
    }
}
