package com.bartekkansy.prism.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * A functional interface for custom 3D rendering.
 */
@FunctionalInterface
public interface IPrismCustomRenderer {
    void render(GuiGraphics guiGraphics, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick);
}