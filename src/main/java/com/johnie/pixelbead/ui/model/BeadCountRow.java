package com.johnie.pixelbead.ui.model;

import com.johnie.pixelbead.engine.model.BeadColor;

/**
 * One row of the bead count table.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/11
 */
public record BeadCountRow(int colorIndex, BeadColor color, int count) {
}
