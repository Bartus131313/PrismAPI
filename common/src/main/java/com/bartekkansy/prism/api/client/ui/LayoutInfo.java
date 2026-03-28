package com.bartekkansy.prism.api.client.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Contains the calculated bounds for a specific UI element.
 */
public record LayoutInfo(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY) {}