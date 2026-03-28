package com.bartekkansy.prism.api.client.render;

import com.bartekkansy.prism.api.util.PrismPlatform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.joml.Matrix4f;

/**
 * Prism API - Core Rendering Utilities
 */
public class PrismRenderer {

    public static void drawFluid(GuiGraphics guiGraphics, Fluid fluid, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0 || fluid == Fluids.EMPTY) return;

        // Get Texture and Color
        IPrismFluidHelper fluidHelper = PrismPlatform.getFluidHelper();
        TextureAtlasSprite sprite = fluidHelper.getStillSprite(fluid);
        int color = fluidHelper.getColor(fluid);

        // Extract RGBA values
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        // Use PositionTexColor to apply tint per-vertex
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);

        // Get the pose and the current Z-level from GuiGraphics
        Matrix4f matrix = guiGraphics.pose().last().pose();
        float zLevel = (float) guiGraphics.pose().last().pose().m23();

        // Get the Tesselator instance
        Tesselator tesselator = Tesselator.getInstance();

        // Begin the builder with QUADS format
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        // Tiling Logic
        for (int drawX = 0; drawX < width; drawX += 16) {
            for (int drawY = 0; drawY < height; drawY += 16) {
                // Calculate how much of this specific 16x16 tile to draw
                int partWidth = Math.min(width - drawX, 16);
                int partHeight = Math.min(height - drawY, 16);

                // Get valid screen coordinates
                int screenX = x + drawX;
                int screenY = y + (height - drawY) - partHeight;

                // Get valid UVs
                float minU = sprite.getU0();
                float maxU = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * (partWidth / 16.0f);
                float minV = sprite.getV0();
                float maxV = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * (partHeight / 16.0f);

                // Add vertices
                builder.addVertex(matrix, screenX, screenY + partHeight, zLevel).setUv(minU, maxV).setColor(r, g, b, a);
                builder.addVertex(matrix, screenX + partWidth, screenY + partHeight, zLevel).setUv(maxU, maxV).setColor(r, g, b, a);
                builder.addVertex(matrix, screenX + partWidth, screenY, zLevel).setUv(maxU, minV).setColor(r, g, b, a);
                builder.addVertex(matrix, screenX, screenY, zLevel).setUv(minU, minV).setColor(r, g, b, a);
            }
        }

        // Upload the buffer to the GPU
        MeshData meshData = builder.build();
        if (meshData != null) BufferUploader.drawWithShader(meshData);

        // No need to reset ShaderColor to 1.0F because we applied the color directly to the vertices.
    }
}