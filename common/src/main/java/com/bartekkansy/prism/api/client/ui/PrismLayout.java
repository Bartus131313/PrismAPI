package com.bartekkansy.prism.api.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A functional layout engine for the Prism API.
 * <p>
 * Instead of drawing immediately, this class stores "blueprints" for UI elements.
 * It calculates the X/Y coordinates based on your desired spacing and "feeds"
 * those coordinates back into your rendering methods via a {@link Consumer}.
 * </p>
 */
public class PrismLayout {
    private final List<Entry> entries = new ArrayList<>();
    private int spacing = 4;

    public PrismLayout() {}

    public PrismLayout(int spacing) {
        this.spacing = spacing;
    }

    /**
     * Internal record to store element dimensions and their rendering logic.
     */
    private record Entry(int width, int height, Consumer<LayoutInfo> renderer) {}

    /**
     * Record passed to the renderer containing calculated bounds and context.
     */
    public record LayoutInfo(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY) {}

    /**
     * Sets the pixel spacing between elements in this layout.
     * @param spacing The gap in pixels.
     * @return This layout instance for chaining.
     */
    public PrismLayout setSpacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    /**
     * Adds an element to the layout queue.
     * * @param width    The width this element will occupy.
     * @param height   The height this element will occupy.
     * @param renderer A lambda calling a {@code void} render method (e.g. PrismRenderer).
     */
    public void addElement(int width, int height, Consumer<LayoutInfo> renderer) {
        entries.add(new Entry(width, height, renderer));
    }

    /**
     * Renders all elements in a horizontal row (Left to Right).
     */
    public void drawHorizontal(GuiGraphics gui, int startX, int startY, int mouseX, int mouseY) {
        int currentX = startX;
        for (Entry entry : entries) {
            LayoutInfo info = new LayoutInfo(gui, currentX, startY, entry.width(), entry.height(), mouseX, mouseY);
            entry.renderer().accept(info);

            // Move X to the right for the next element
            currentX += entry.width() + spacing;
        }
    }

    /**
     * Renders all elements in a vertical column (Top to Bottom).
     */
    public void drawVertical(GuiGraphics gui, int startX, int startY, int mouseX, int mouseY) {
        int currentY = startY;
        for (Entry entry : entries) {
            LayoutInfo info = new LayoutInfo(gui, startX, currentY, entry.width(), entry.height(), mouseX, mouseY);
            entry.renderer().accept(info);

            // Move Y down for the next element
            currentY += entry.height() + spacing;
        }
    }

    /**
     * Calculates the total width of all elements plus spacing.
     * Useful for centering a row on the screen.
     */
    public int getTotalWidth() {
        if (entries.isEmpty()) return 0;
        int totalWidth = entries.stream().mapToInt(Entry::width).sum();
        return totalWidth + (spacing * (entries.size() - 1));
    }

    /**
     * Calculates the total height of all elements plus spacing.
     * Useful for centering a column on the screen.
     */
    public int getTotalHeight() {
        if (entries.isEmpty()) return 0;
        int totalHeight = entries.stream().mapToInt(Entry::height).sum();
        return totalHeight + (spacing * (entries.size() - 1));
    }

    /**
     * Clears all elements from the layout.
     */
    public void clear() {
        this.entries.clear();
    }
}