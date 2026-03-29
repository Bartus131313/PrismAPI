package com.bartekkansy.prism.api.client.render;

import com.bartekkansy.prism.api.client.ui.PrismAnimation;
import com.bartekkansy.prism.api.client.ui.PrismDirection;
import com.bartekkansy.prism.api.fluid.FluidTankInfo;
import com.bartekkansy.prism.api.fluid.IPrismFluidHelper;
import com.bartekkansy.prism.api.util.PrismNumberFormatter;
import com.bartekkansy.prism.api.util.PrismPlatform;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Core rendering utility for the Prism API.
 * Provides high-performance, platform-agnostic methods for drawing UI elements
 * using the {@link Tesselator} and {@link BufferBuilder}.
 */
public class PrismRenderer {

    /**
     * Renders a tiled fluid texture at the specified coordinates.
     * <p>
     * This method utilizes vertex-level color tinting and is optimized to batch
     * draw calls for the entire area. The texture is automatically tiled based on
     * the standard 16x16 pixel sprite size.
     * </p>
     *
     * @param guiGraphics The current {@link GuiGraphics} instance.
     * @param fluid       The {@link Fluid} to render.
     * @param x           The X-coordinate on the screen.
     * @param y           The Y-coordinate on the screen.
     * @param width       The width of the rendering area.
     * @param height      The height of the rendering area.
     */
    public static void renderFluidTexture(GuiGraphics guiGraphics, Fluid fluid, int x, int y, int width, int height) {
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
                int partWidth = Math.min(width - drawX, 16);
                int partHeight = Math.min(height - drawY, 16);

                // Calculate how much of this specific 16x16 tile to draw
                int screenX = x + drawX;
                int screenY = y + (height - drawY) - partHeight;

                // Get valid UVs
                float minU = sprite.getU0();
                float maxU = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * (partWidth / 16.0f);
                float maxV = sprite.getV1();
                float minV = sprite.getV1() - (sprite.getV1() - sprite.getV0()) * (partHeight / 16.0f);

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
    }

    /**
     * Renders a fluid tank with its height scaled relative to its capacity.
     * <p>
     * The fluid is rendered from the bottom-up, ensuring the visual level
     * accurately reflects the fill percentage.
     * </p>
     *
     * @param guiGraphics The current {@link GuiGraphics} instance.
     * @param fluid       The {@link Fluid} contained in the tank.
     * @param amount      The current amount of fluid (e.g., in mB).
     * @param maxAmount   The maximum capacity of the tank.
     * @param x           The X-coordinate on the screen.
     * @param y           The Y-coordinate on the screen.
     * @param width       The width of the tank.
     * @param height      The total height of the tank.
     */
    public static void renderFluidTank(GuiGraphics guiGraphics, Fluid fluid, int amount, int maxAmount, int x, int y,
                                       int width, int height) {
        if (amount <= 0 || fluid == Fluids.EMPTY) return;

        // Calculate the scaling (How high is the fluid?)
        float capacityRatio = (float) amount / maxAmount;
        int fluidHeight = (int) (height * capacityRatio);

        // Adjust Y so it draws from the BOTTOM of the tank up
        int fluidY = y + (height - fluidHeight);

        // Call your existing high-performance renderer
        renderFluidTexture(guiGraphics, fluid, x, fluidY, width, fluidHeight);
    }

    /**
     * Renders a fluid tank and displays a custom tooltip when hovered by the mouse.
     * <p>
     * This method uses a {@link Function} provider to allow developers to construct
     * dynamic, multi-line tooltips using the current fluid state.
     * </p>
     *
     * @param guiGraphics     The current {@link GuiGraphics} instance.
     * @param fluid           The {@link Fluid} to render.
     * @param amount          Current volume.
     * @param maxAmount       Max capacity.
     * @param x               The X-coordinate.
     * @param y               The Y-coordinate.
     * @param width           Tank width.
     * @param height          Tank height.
     * @param mouseX          Current mouse X-coordinate.
     * @param mouseY          Current mouse Y-coordinate.
     * @param tooltipProvider A function returning a list of {@link Component}s based on {@link FluidTankInfo}.
     */
    public static void renderFluidTankWithTooltip(GuiGraphics guiGraphics, Fluid fluid, int amount, int maxAmount,
                                                  int x, int y, int width, int height, int mouseX, int mouseY,
                                                  Function<FluidTankInfo, List<Component>> tooltipProvider) {
        // Always draw the fluid first
        renderFluidTank(guiGraphics, fluid, amount, maxAmount, x, y, width, height);

        // Get the custom list of components
        FluidTankInfo info = new FluidTankInfo(fluid, amount, maxAmount);
        List<Component> customTooltip = tooltipProvider.apply(info);

        renderContainerTooltip(guiGraphics, x, y, width, height, mouseX, mouseY, customTooltip);
    }

    /**
     * Renders a fluid tank with a standard "Name: Amount / Capacity mB" tooltip.
     * <p>
     * This is a convenience method for common use cases where custom formatting
     * is not required.
     * </p>

     * @param guiGraphics The current {@link GuiGraphics} instance.
     * @param fluid       The {@link Fluid} to render.
     * @param amount      Current volume.
     * @param maxAmount   Max capacity.
     * @param x           The X-coordinate.
     * @param y           The Y-coordinate.
     * @param width       Tank width.
     * @param height      Tank height.
     * @param mouseX      Current mouse X-coordinate.
     * @param mouseY      Current mouse Y-coordinate.
     * @apiNote {@link #renderFluidTankWithTooltip(GuiGraphics, Fluid, int, int, int, int, int, int, int, int, Function)}
     */
    public static void renderFluidTankWithTooltip(GuiGraphics guiGraphics, Fluid fluid, int amount, int maxAmount,
                                                  int x, int y, int width, int height, int mouseX, int mouseY) {

        PrismRenderer.renderFluidTankWithTooltip(guiGraphics, fluid, amount, maxAmount, x, y, width, height, mouseX, mouseY, (info) -> {
            List<Component> lines = new ArrayList<>();

            // Create the tooltip text (e.g., "Lava: 500 / 1000 mB")
            lines.add(Component.empty().append(info.getDisplayName())
                    .append(Component.literal(": " + PrismNumberFormatter.format(amount) + " / " + PrismNumberFormatter.format(maxAmount) + " mB"))
                    .withStyle(ChatFormatting.GRAY));

            return lines;
        });
    }

    /**
     * Renders a multi-line tooltip if the mouse is hovering over the specified rectangular area.
     * <p>
     * This is a universal hit-box utility that simplifies adding tooltips to custom
     * UI elements like progress bars, icons, or decorative frames.
     * </p>
     *
     * @param guiGraphics The current {@link GuiGraphics} instance.
     * @param x           The X-coordinate of the hover area's top-left corner.
     * @param y           The Y-coordinate of the hover area's top-left corner.
     * @param width       The width of the hover area.
     * @param height      The height of the hover area.
     * @param mouseX      The current mouse X-coordinate (usually from the render method).
     * @param mouseY      The current mouse Y-coordinate (usually from the render method).
     * @param tooltip     A {@link List} of {@link Component}s representing the lines of the tooltip.
     *
     * @apiNote Use this for complex tooltips that require multiple colors or descriptions.
     */
    public static void renderContainerTooltip(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, List<Component> tooltip) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
        }
    }

    /**
     * Renders a single-line tooltip if the mouse is hovering over the specified rectangular area.
     * <p>
     * A convenience overload for simple labels or names that do not require multiple lines.
     * </p>
     *
     * @param tooltip     A single {@link Component} representing the tooltip text.
     * @see #renderContainerTooltip(GuiGraphics, int, int, int, int, int, int, List)
     */
    public static void renderContainerTooltip(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, Component tooltip) {
        renderContainerTooltip(guiGraphics, x, y, width, height, mouseX, mouseY, List.of(tooltip));
    }

    /**
     * Renders a vertical procedural container that fills from the bottom upwards.
     * <p>
     * This method uses a vertical gradient to create depth. The fill logic automatically
     * calculates the top-offset to ensure the bar "grows" from the base coordinate.
     * </p>
     *
     * @param guiGraphics The current {@link GuiGraphics} instance.
     * @param x           The X-coordinate of the container.
     * @param y           The Y-coordinate (top) of the container.
     * @param width       The width of the bar.
     * @param height      The total height of the bar.
     * @param fillAmount  A float between {@code 0.0f} and {@code 1.0f} representing the fill level.
     * @param colorStart  The ARGB color at the top of the gradient.
     * @param colorEnd    The ARGB color at the bottom of the gradient.
     *
     * @apiNote Example: {@code renderVerticalContainer(gui, x, y, 10, 50, 0.5f, 0xFF00FF00, 0xFF004400);}
     */
    public static void renderVerticalFillContainer(GuiGraphics guiGraphics, int x, int y, int width, int height, float fillAmount, int colorStart, int colorEnd) {
        if (fillAmount <= 0.0f) return;
        fillAmount = Math.min(fillAmount, 1.0f);

        int fillHeight = (int) (height * fillAmount);
        int top = y + (height - fillHeight);
        int bottom = y + height; // Absolute bottom coordinate

        guiGraphics.fillGradient(x, top, x + width, bottom, colorStart, colorEnd);
    }

    /**
     * Renders a vertical procedural container using {@link Color} objects for the gradient.
     *
     * @see #renderVerticalFillContainer(GuiGraphics, int, int, int, int, float, int, int)
     */
    public static void renderVerticalFillContainer(GuiGraphics guiGraphics, int x, int y, int width, int height, float fillAmount, Color colorStart, Color colorEnd) {
        renderVerticalFillContainer(guiGraphics, x, y, width, height, fillAmount, colorStart.getRGB(), colorEnd.getRGB());
    }

    /**
     * Renders a vertical procedural container with a single solid {@link Color}.
     *
     * @see #renderVerticalFillContainer(GuiGraphics, int, int, int, int, float, int, int)
     */
    public static void renderVerticalFillContainer(GuiGraphics guiGraphics, int x, int y, int width, int height, float fillAmount, Color color) {
        renderVerticalFillContainer(guiGraphics, x, y, width, height, fillAmount, color.getRGB(), color.getRGB());
    }

    /**
     * Renders a horizontal procedural container that fills from left to right.
     * <p>
     * Ideal for progress arrows or health bars. Uses a vertical gradient within the horizontal
     * fill to provide a "cylindrical" or shaded look.
     * </p>
     *
     * @param guiGraphics The current {@link GuiGraphics} instance.
     * @param x           The X-coordinate (left) of the container.
     * @param y           The Y-coordinate of the container.
     * @param width       The total width of the container.
     * @param height      The height of the bar.
     * @param fillAmount  A float between {@code 0.0f} and {@code 1.0f} representing the fill level.
     * @param colorStart  The ARGB color at the top of the bar's gradient.
     * @param colorEnd    The ARGB color at the bottom of the bar's gradient.
     *
     * @apiNote Example: {@code renderHorizontalFillContainer(gui, x, y, 100, 10, 0.75f, 0xFFFF0000, 0xFF440000);}
     */
    public static void renderHorizontalFillContainer(GuiGraphics guiGraphics, int x, int y, int width, int height, float fillAmount, int colorStart, int colorEnd) {
        if (fillAmount <= 0.0f) return;
        fillAmount = Math.min(fillAmount, 1.0f);

        int fillWidth = (int) (width * fillAmount);
        int right = x + fillWidth; // Absolute right coordinate

        guiGraphics.fillGradient(x, y, right, y + height, colorStart, colorEnd);
    }

    /**
     * Renders a horizontal procedural container using {@link Color} objects for the gradient.
     *
     * @see #renderHorizontalFillContainer(GuiGraphics, int, int, int, int, float, int, int)
     */
    public static void renderHorizontalFillContainer(GuiGraphics guiGraphics, int x, int y, int width, int height, float fillAmount, Color colorStart, Color colorEnd) {
        renderHorizontalFillContainer(guiGraphics, x, y, width, height, fillAmount, colorStart.getRGB(), colorEnd.getRGB());
    }

    /**
     * Renders a horizontal procedural container with a single solid {@link Color}.
     *
     * @see #renderHorizontalFillContainer(GuiGraphics, int, int, int, int, float, int, int)
     */
    public static void renderHorizontalFillContainer(GuiGraphics guiGraphics, int x, int y, int width, int height, float fillAmount, Color color) {
        renderHorizontalFillContainer(guiGraphics, x, y, width, height, fillAmount, color.getRGB(), color.getRGB());
    }

    /**
     * Renders a vertical textured bar that fills from the bottom upwards.
     * <p>
     * This method performs UV clipping on the source texture. It ensures that as the
     * bar fills, the texture remains anchored to the bottom of the container,
     * effectively "revealing" the sprite from the base up.
     * </p>
     *
     * @param guiGraphics The current {@link GuiGraphics} instance.
     * @param texture     The {@link ResourceLocation} of the GUI texture sheet.
     * @param x           The X-coordinate on the screen.
     * @param y           The Y-coordinate (top) of the container.
     * @param width       The width of the bar.
     * @param height      The total height of the bar.
     * @param fillAmount  A float between {@code 0.0f} and {@code 1.0f} representing the fill level.
     * @param u           The X-coordinate of the "full" sprite on the texture sheet.
     * @param v           The Y-coordinate (top) of the "full" sprite on the texture sheet.
     *
     * @apiNote Useful for vertical energy meters or thermometer-style displays.
     */
    public static void renderVerticalFillTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height, float fillAmount, int u, int v) {
        if (fillAmount <= 0.0f) return;
        fillAmount = Math.min(fillAmount, 1.0f);

        int fillHeight = (int) (height * fillAmount);

        // Calculate the start point of the "filled" part
        int screenY = y + (height - fillHeight);
        int textureV = v + (height - fillHeight);

        guiGraphics.blit(texture, x, screenY, u, textureV, width, fillHeight);
    }

    /**
     * Renders a horizontal textured bar that fills from left to right.
     * <p>
     * This method clips the width of the rendered sprite based on the fill percentage.
     * The left edge of the texture remains anchored to the X-coordinate.
     * </p>
     *
     * @param guiGraphics The current {@link GuiGraphics} instance.
     * @param texture     The {@link ResourceLocation} of the GUI texture sheet.
     * @param x           The X-coordinate (left) on the screen.
     * @param y           The Y-coordinate on the screen.
     * @param width       The total width of the bar.
     * @param height      The height of the bar.
     * @param fillAmount  A float between {@code 0.0f} and {@code 1.0f} representing the fill level.
     * @param u           The X-coordinate of the "full" sprite on the texture sheet.
     * @param v           The Y-coordinate of the "full" sprite on the texture sheet.
     *
     * @apiNote Standard choice for horizontal progress arrows or health/mana bars.
     */
    public static void renderHorizontalFillTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height, float fillAmount, int u, int v) {
        if (fillAmount <= 0.0f) return;
        fillAmount = Math.min(fillAmount, 1.0f);

        int fillWidth = (int) (width * fillAmount);

        guiGraphics.blit(texture, x, y, u, v, fillWidth, height);
    }

    /**
     * Renders a procedural progress bar that fills in the specified {@link PrismDirection}.
     * <p>
     * This is a universal helper that handles the coordinate math for all four cardinal directions.
     * It uses a vertical gradient (top-to-bottom) within the filled area to provide visual depth.
     * </p>
     *
     * @param guiGraphics The current {@link GuiGraphics} instance.
     * @param x           The X-coordinate of the bar's top-left corner.
     * @param y           The Y-coordinate of the bar's top-left corner.
     * @param width       The total width of the bar container.
     * @param height      The total height of the bar container.
     * @param progress    A float between {@code 0.0f} and {@code 1.0f} representing the fill level.
     * @param colorStart  The ARGB color at the top of the bar's gradient.
     * @param colorEnd    The ARGB color at the bottom of the bar's gradient.
     * @param direction   The {@link PrismDirection} in which the bar should fill.
     * @apiNote To create a bar that fills from the center outwards, use two separate calls
     * with {@link PrismDirection#LEFT} and {@link PrismDirection#RIGHT}.
     */
    public static void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress,
                                         int colorStart, int colorEnd, PrismDirection direction) {
        switch (direction) {
            case UP -> renderVerticalFillContainer(guiGraphics, x, y, width, height, progress, colorStart, colorEnd);
            case RIGHT -> renderHorizontalFillContainer(guiGraphics, x, y, width, height, progress, colorStart, colorEnd);
            case DOWN -> {
                int fillHeight = (int) (height * Math.min(progress, 1.0f));
                guiGraphics.fillGradient(x, y, x + width, y + fillHeight, colorStart, colorEnd);
            }
            case LEFT -> {
                int fillWidth = (int) (width * Math.min(progress, 1.0f));
                guiGraphics.fillGradient(x + (width - fillWidth), y, x + width, y + height, colorStart, colorEnd);
            }
        }
    }

    /**
     * Renders a progress bar using {@link Color} objects for the gradient.
     *
     * @see #renderProgressBar(GuiGraphics, int, int, int, int, float, int, int, PrismDirection)
     */
    public static void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress,
                                         Color colorStart, Color colorEnd, PrismDirection direction) {
        renderProgressBar(guiGraphics, x, y, width, height, progress, colorStart.getRGB(), colorEnd.getRGB(), direction);
    }
    /**
     * Renders a progress bar using a single solid {@link Color}.
     *
     * @see #renderProgressBar(GuiGraphics, int, int, int, int, float, int, int, PrismDirection)
     */
    public static void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress,
                                         Color color, PrismDirection direction) {
        renderProgressBar(guiGraphics, x, y, width, height, progress, color.getRGB(), color.getRGB(), direction);
    }

    /**
     * Renders a textured progress bar that fills in the specified {@link PrismDirection}.
     * <p>
     * This method automatically clips the source texture and offsets screen coordinates
     * based on the chosen direction, ensuring the texture remains anchored correctly
     * (e.g., anchoring to the bottom for {@code UP} or the right for {@code LEFT}).
     * </p>
     *
     * @param guiGraphics The current {@link GuiGraphics} instance.
     * @param texture     The {@link ResourceLocation} of the GUI texture sheet.
     * @param x           The X-coordinate of the bar's top-left corner.
     * @param y           The Y-coordinate of the bar's top-left corner.
     * @param width       The total width of the bar container.
     * @param height      The total height of the bar container.
     * @param progress    A float between {@code 0.0f} and {@code 1.0f} representing the fill level.
     * @param u           The X-coordinate of the "full" sprite on the texture sheet.
     * @param v           The Y-coordinate of the "full" sprite on the texture sheet.
     * @param direction   The {@link PrismDirection} in which the bar should fill.
     *
     * @see PrismDirection
     * @apiNote Ensure the {@code u} and {@code v} parameters point to the top-left
     * of the <b>fully filled</b> version of your sprite.
     */
    public static void renderProgressBarTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height, float progress,
                                                int u, int v, PrismDirection direction) {
        switch (direction) {
            case UP -> renderVerticalFillTexture(guiGraphics, texture, x, y, width, height, progress, u, v);
            case RIGHT -> renderHorizontalFillTexture(guiGraphics, texture, x, y, width, height, progress, u, v);
            case DOWN -> {
                int fillHeight = (int) (height * Math.min(progress, 1.0f));
                guiGraphics.blit(texture, x, y, u, v, width, fillHeight);
            }
            case LEFT -> {
                int fillWidth = (int) (width * Math.min(progress, 1.0f));
                guiGraphics.blit(texture, x + (width - fillWidth), y, u + (width - fillWidth), v, fillWidth, height);
            }
        }
    }

    /**
     * Renders a string with a specific scale and ARGB color.
     * <p>
     * This method handles the coordinate transformation by pushing a new pose,
     * translating to the target coordinates, and applying the scale factor.
     * </p>
     *
     * @param guiGraphics The current {@link GuiGraphics} instance.
     * @param font        The {@link Font} renderer.
     * @param string      The {@link Component} to render.
     * @param x           The X-coordinate (left-aligned anchor).
     * @param y           The Y-coordinate (top-aligned anchor).
     * @param scale       The scale factor (e.g., {@code 0.5f} for half size).
     * @param color       The ARGB integer color.
     * @param shadow      Whether to render a drop-shadow.
     */
    public static void renderString(GuiGraphics guiGraphics, Font font, Component string, int x, int y, float scale, int color, boolean shadow) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, scale);

        guiGraphics.drawString(font, string, 0, 0, color, shadow);

        guiGraphics.pose().popPose();
    }

    /**
     * Renders a string with a specific scale using a {@link Color} object.
     *
     * @see #renderString(GuiGraphics, Font, Component, int, int, float, int, boolean)
     */
    public static void renderString(GuiGraphics guiGraphics, Font font, Component string, int x, int y, float scale, Color color, boolean shadow) {
        renderString(guiGraphics, font, string, x, y, scale, color.getRGB(), shadow);
    }

    /**
     * Renders a string centered horizontally relative to the provided X coordinate.
     * <p>
     * The method calculates the total width of the string multiplied by the scale
     * to ensure the center remains consistent even when the text is resized.
     * </p>
     *
     * @param x The horizontal center point.
     * @see #renderString(GuiGraphics, Font, Component, int, int, float, int, boolean)
     */
    public static void renderStringCenteredX(GuiGraphics guiGraphics, Font font, Component string, int x, int y, float scale, int color, boolean shadow) {
        float width = font.width(string) * scale;
        float adjustedX = x - (width / 2f);
        renderString(guiGraphics, font, string, (int) adjustedX, y, scale, color, shadow);
    }

    /**
     * Renders a string centered horizontally using a {@link Color} object.
     *
     * @see #renderStringCenteredX(GuiGraphics, Font, Component, int, int, float, int, boolean)
     */
    public static void renderStringCenteredX(GuiGraphics guiGraphics, Font font, Component string, int x, int y, float scale, Color color, boolean shadow) {
        renderStringCenteredX(guiGraphics, font, string, x, y, scale, color.getRGB(), shadow);
    }

    /**
     * Renders a string centered vertically relative to the provided Y coordinate.
     * <p>
     * Useful for aligning labels inside bars or buttons where the text needs
     * to be centered between the top and bottom borders.
     * </p>
     *
     * @param y The vertical center point.
     * @see #renderString(GuiGraphics, Font, Component, int, int, float, int, boolean)
     */
    public static void renderStringCenteredY(GuiGraphics guiGraphics, Font font, Component string, int x, int y, float scale, int color, boolean shadow) {
        float height = font.lineHeight * scale;
        float adjustedY = y - (height / 2f);
        renderString(guiGraphics, font, string, x, (int) adjustedY, scale, color, shadow);
    }

    /**
     * Renders a string centered vertically using a {@link Color} object.
     *
     * @see #renderStringCenteredY(GuiGraphics, Font, Component, int, int, float, int, boolean)
     */
    public static void renderStringCenteredY(GuiGraphics guiGraphics, Font font, Component string, int x, int y, float scale, Color color, boolean shadow) {
        renderStringCenteredY(guiGraphics, font, string, x, y, scale, color.getRGB(), shadow);
    }

    /**
     * Renders a string perfectly centered on both the X and Y axes.
     * <p>
     * This is the standard choice for titles, icon labels, and tooltips
     * where the text must be perfectly balanced within a bounding box.
     * </p>
     *
     * @param x The horizontal center point.
     * @param y The vertical center point.
     * @see #renderString(GuiGraphics, Font, Component, int, int, float, int, boolean)
     */
    public static void renderStringCenteredXY(GuiGraphics guiGraphics, Font font, Component string, int x, int y, float scale, int color, boolean shadow) {
        float width = font.width(string) * scale;
        float height = font.lineHeight * scale;
        renderString(guiGraphics, font, string, (int)(x - width / 2f), (int)(y - height / 2f), scale, color, shadow);
    }

    /**
     * Renders a string perfectly centered using a {@link Color} object.
     *
     * @see #renderStringCenteredXY(GuiGraphics, Font, Component, int, int, float, int, boolean)
     */
    public static void renderStringCenteredXY(GuiGraphics guiGraphics, Font font, Component string, int x, int y, float scale, Color color, boolean shadow) {
        renderStringCenteredXY(guiGraphics, font, string, x, y, scale, color.getRGB(), shadow);
    }

    /**
     * Starts a scissor cut on the screen. Any rendering calls made after this
     * and before {@link #stopScissor(GuiGraphics)} will be clipped to this area.
     * @param x      The X-coordinate of the top-left corner.
     * @param y      The Y-coordinate of the top-left corner.
     * @param width  The width of the visible window.
     * @param height The height of the visible window.
     */
    public static void startScissor(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Minecraft's enableScissor uses absolute screen coordinates.
        // It's best to use the built-in GuiGraphics helper which handles scaling.
        guiGraphics.enableScissor(x, y, x + width, y + height);
    }

    /**
     * Disables the current scissor cut and restores full-screen rendering.
     */
    public static void stopScissor(GuiGraphics guiGraphics) {
        guiGraphics.disableScissor();
    }

    /**
     * Renders a Component with a color gradient, multiplied by the Component's intrinsic style color.
     * If the Component color is White, the gradient is rendered normally.
     *
     * @param guiGraphics The vanilla GuiGraphics instance.
     * @param font        The font renderer.
     * @param text        The Component to render (supports nested styles/colors).
     * @param x           X position.
     * @param y           Y position.
     * @param scale       Text scale.
     * @param colorStart  Starting gradient color (ARGB).
     * @param colorEnd    Ending gradient color (ARGB).
     * @param shadow      Whether to draw a text shadow.
     */
    public static void renderGradientString(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, int colorStart, int colorEnd, boolean shadow) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);

        float totalWidth = font.width(text);
        final float[] currentX = {0}; // Array used to allow modification inside lambda

        // Start/End color extraction
        int r0 = (colorStart >> 16) & 0xFF, g0 = (colorStart >> 8) & 0xFF, b0 = colorStart & 0xFF, a0 = (colorStart >> 24) & 0xFF;
        int r1 = (colorEnd >> 16) & 0xFF, g1 = (colorEnd >> 8) & 0xFF, b1 = colorEnd & 0xFF, a1 = (colorEnd >> 24) & 0xFF;

        text.getVisualOrderText().accept((index, style, codePoint) -> {
            String ch = new String(Character.toChars(codePoint));
            float delta = currentX[0] / Math.max(1, totalWidth);

            // Interpolate Gradient
            int gr = (int) PrismAnimation.lerp(r0, r1, delta);
            int gg = (int) PrismAnimation.lerp(g0, g1, delta);
            int gb = (int) PrismAnimation.lerp(b0, b1, delta);
            int ga = (int) PrismAnimation.lerp(a0, a1, delta);

            // Component Tint (White = 1.0)
            int cCol = style.getColor() != null ? style.getColor().getValue() : 0xFFFFFF;

            // Multiply channels
            int fr = (gr * ((cCol >> 16) & 0xFF)) / 255;
            int fg = (gg * ((cCol >> 8) & 0xFF)) / 255;
            int fb = (gb * (cCol & 0xFF)) / 255;

            guiGraphics.drawString(font, ch, (int)currentX[0], 0, (ga << 24) | (fr << 16) | (fg << 8) | fb, shadow);
            currentX[0] += font.width(FormattedCharSequence.forward(ch, style));
            return true;
        });

        guiGraphics.pose().popPose();
    }

    /**
     * Renders a gradient colored string using a {@link Color} objects.
     *
     * @see #renderGradientString(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderGradientString(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, Color colorStart, Color colorEnd, boolean shadow) {
        renderGradientString(guiGraphics, font, text, x, y, scale, colorStart.getRGB(), colorEnd.getRGB(), shadow);
    }

    /**
     * Renders a string centered horizontally relative to the provided X coordinate.
     * Iterates through each character to calculate a unique interpolated color.
     * <p>
     * The method calculates the total width of the string multiplied by the scale
     * to ensure the center remains consistent even when the text is resized.
     * </p>
     *
     * @param x The horizontal center point.
     * @see #renderGradientString(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderGradientStringCenteredX(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, int colorStart, int colorEnd, boolean shadow) {
        float width = font.width(text) * scale;
        renderGradientString(guiGraphics, font, text, (int)(x - width / 2), y, scale, colorStart, colorEnd, shadow);
    }

    /**
     * Renders a string centered horizontally using a gradient with {@link Color} objects.
     *
     * @see #renderGradientStringCenteredX(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderGradientStringCenteredX(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, Color colorStart, Color colorEnd, boolean shadow) {
        renderGradientStringCenteredX(guiGraphics, font, text, x, y, scale, colorStart.getRGB(), colorEnd.getRGB(), shadow);
    }

    /**
     * Renders a string centered vertically relative to the provided Y coordinate.
     * Iterates through each character to calculate a unique interpolated color.
     * <p>
     * Useful for aligning labels inside bars or buttons where the text needs
     * to be centered between the top and bottom borders.
     * </p>
     *
     * @param y The vertical center point.
     * @see #renderGradientString(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderGradientStringCenteredY(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, int colorStart, int colorEnd, boolean shadow) {
        float height = font.lineHeight * scale;
        renderGradientString(guiGraphics, font, text, x, (int)(y - height / 2), scale, colorStart, colorEnd, shadow);
    }

    /**
     * Renders a string centered vertically using a gradient with {@link Color} objects.
     *
     * @see #renderGradientStringCenteredY(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderGradientStringCenteredY(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, Color colorStart, Color colorEnd, boolean shadow) {
        renderGradientStringCenteredY(guiGraphics, font, text, x, y, scale, colorStart.getRGB(), colorEnd.getRGB(), shadow);
    }

    /**
     * Renders a string perfectly centered on both the X and Y axes.
     * Iterates through each character to calculate a unique interpolated color.
     * <p>
     * This is the standard choice for titles, icon labels, and tooltips
     * where the text must be perfectly balanced within a bounding box.
     * </p>
     *
     * @param x The horizontal center point.
     * @param y The vertical center point.
     * @see #renderGradientString(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderGradientStringCenteredXY(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, int colorStart, int colorEnd, boolean shadow) {
        float width = font.width(text) * scale;
        float height = font.lineHeight * scale;
        renderGradientString(guiGraphics, font, text, (int)(x - width / 2), (int)(y - height / 2), scale, colorStart, colorEnd, shadow);
    }

    /**
     * Renders a string centered vertically and horizontally using a gradient with {@link Color} objects.
     *
     * @see #renderGradientStringCenteredXY(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderGradientStringCenteredXY(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, Color colorStart, Color colorEnd, boolean shadow) {
        renderGradientStringCenteredXY(guiGraphics, font, text, x, y, scale, colorStart.getRGB(), colorEnd.getRGB(), shadow);
    }

    /**
     * Renders a single Block/Item icon centered at a specific GUI position.
     * <p>
     * Unlike vanilla item rendering, this method centers the block on the provided coordinates
     * and automatically adjusts lighting based on whether the model is a solid block
     * (3D lighting) or a flat item/plant (Flat lighting).
     *
     * @param guiGraphics The current GuiGraphics instance from the render call.
     * @param block       The block to be rendered.
     * @param x           The screen X-coordinate for the CENTER of the block.
     * @param y           The screen Y-coordinate for the CENTER of the block.
     * @param scale       The scale multiplier (1.0 = 16x16 pixels).
     */
    public static void renderBlockAsItem(GuiGraphics guiGraphics, Block block, int x, int y, float scale) {
        ItemStack stack = block.asItem().getDefaultInstance();
        if (stack.isEmpty()) return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // Move exactly to X and Y.
        // Because of how the scaling works next, this X and Y is now the absolute CENTER of the block.
        poseStack.translate(x, y, 150.0F);

        // Scale and Flip.
        // - Vanilla base size is 16. We multiply by your custom scale.
        // - We make the Y-scale NEGATIVE to flip the 3D block right-side up for the GUI.
        float finalScale = 16.0F * scale;
        poseStack.scale(finalScale, -finalScale, finalScale);

        // Setup rendering components
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, null, null, 0);

        // Dynamic Lighting
        // Solid blocks need 3D lighting, but flat blocks (like Saplings/Flowers) need flat lighting.
        boolean usesBlockLight = model.usesBlockLight();
        if (usesBlockLight) {
            Lighting.setupFor3DItems();
        } else {
            Lighting.setupForFlatItems();
        }

        MultiBufferSource.BufferSource bufferSource = guiGraphics.bufferSource();

        // Render the Model
        itemRenderer.render(stack, ItemDisplayContext.GUI, false, poseStack, bufferSource,
                15728880, // Full Brightness
                OverlayTexture.NO_OVERLAY,
                model
        );

        // Flush to draw immediately
        guiGraphics.flush();

        // Reset lighting back to default for the rest of the UI
        Lighting.setupFor3DItems();

        poseStack.popPose();
    }

    /**
     * Initializes a 3D rendering context within a 2D GUI.
     * <p>
     * This method "transforms" the screen at the specified (x, y) into a 3D world space.
     * It enables the Depth Buffer, allowing blocks rendered with {@link #renderBlock3D}
     * to overlap correctly regardless of draw order.
     * <p>
     * <b>Note:</b> Every call to {@code enableCamera} MUST be followed by a {@link #disableCamera} call.
     *
     * @param guiGraphics The current GuiGraphics instance.
     * @param x           The screen X-position where the 3D structure's origin will be.
     * @param y           The screen Y-position where the 3D structure's origin will be.
     * @param scale       Global scale of the 3D objects.
     * @param rotX        Camera rotation around the X-axis (Pitch). Try 30.0F for isometric.
     * @param rotY        Camera rotation around the Y-axis (Yaw). Try 225.0F for isometric.
     */
    public static void enableCamera(GuiGraphics guiGraphics, int x, int y, float scale, float rotX, float rotY) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // Move camera to the X/Y position on the screen
        // We push Z to 200 so the blocks have 3D space to exist without clipping into the background
        poseStack.translate(x, y, 200.0F);

        // Scale up to block size and flip the Y axis (GUI is Y-down, World is Y-up)
        float finalScale = 16.0F * scale;
        poseStack.scale(finalScale, -finalScale, finalScale);

        // Apply Camera Rotation
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));

        // RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // Enable Depth Buffer (The magic that makes overlapping blocks work)
        RenderSystem.enableDepthTest();

        // Setup lighting so blocks aren't pitch black
        Lighting.setupFor3DItems();
    }

    /**
     * Finalizes the 3D rendering context and flushes the buffer to the screen.
     * <p>
     * This method resets the RenderSystem's depth state and lighting to prevent
     * 3D settings from "leaking" into the rest of the 2D GUI (like text or buttons).
     *
     * @param guiGraphics The current GuiGraphics instance.
     * @throws IllegalStateException if called without a preceding {@link #enableCamera} call.
     */
    public static void disableCamera(GuiGraphics guiGraphics) {
        // We must end the batch before popping the pose!
        // If we pop the pose before flushing, the blocks will draw in the wrong place.
        guiGraphics.bufferSource().endBatch();

        // Turn off 3D depth so normal tooltips and text don't break
        RenderSystem.disableDepthTest();

        // Pop the camera matrix
        guiGraphics.pose().popPose();
    }

    /**
     * Renders a specific BlockState at a grid-based coordinate within an active camera context.
     * <p>
     * Coordinates are in "Block Units" where 1.0 represents the width of one full block.
     * The block is automatically centered so that (0, 0, 0) is the middle of the structure.
     *
     * @param guiGraphics The current GuiGraphics instance.
     * @param state       The specific BlockState to render (supports rotation, properties, etc.).
     * @param gridX       Position on the X-axis (Right/Left).
     * @param gridY       Position on the Y-axis (Up/Down).
     * @param gridZ       Position on the Z-axis (Forward/Back).
     * @see #enableCamera(GuiGraphics, int, int, float, float, float)
     */
    public static void renderBlock3D(GuiGraphics guiGraphics, BlockState state, float gridX, float gridY, float gridZ) {
        PoseStack poseStack = guiGraphics.pose();

        // Push a temporary pose just for this single block
        poseStack.pushPose();

        // Translate by standard Minecraft block coordinates (1.0 = 1 block)
        // We center the block by shifting it by -0.5, allowing structures to rotate nicely around their center
        poseStack.translate(gridX - 0.5f, gridY - 0.5f, gridZ - 0.5f);

        // Ensure the correct shader is set for 3D blocks
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        MultiBufferSource.BufferSource bufferSource = guiGraphics.bufferSource();

        // Get the correct buffer for the block's render type (Solid, Cutout, etc.)
        // If we use a generic buffer, complex blocks like Pistons/Glass often turn invisible.
        RenderType type = ItemBlockRenderTypes.getChunkRenderType(state);
        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(type);

        // Render the BlockState directly
        dispatcher.renderSingleBlock(state, poseStack, bufferSource,
                15728880, // Full Brightness
                OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    /**
     * Overload for {@link #renderBlock3D(GuiGraphics, BlockState, float, float, float)}
     * using the default state of a Block.
     */
    public static void renderBlock3D(GuiGraphics guiGraphics, Block block, float gridX, float gridY, float gridZ) {
        renderBlock3D(guiGraphics, block.defaultBlockState(), gridX, gridY, gridZ);
    }

    /**
     * Renders a piston extending or retracting.
     * @param progress 0.0 (retracted) to 1.0 (fully extended)
     * @param direction The direction the piston is facing (e.g., Direction.UP)
     */
    public static void renderPiston3D(GuiGraphics guiGraphics, Direction direction, float progress, float gridX, float gridY, float gridZ) {
        // Determine if we should hide the head on the base
        // If progress is 0, we show the normal retracted piston (wood included).
        // If progress > 0, we must set EXTENDED to true to hide the base's wooden part.
        boolean isExtended = progress > 0.0f;

        // Prepare the Base State
        BlockState baseState = Blocks.PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, direction)
                .setValue(PistonBaseBlock.EXTENDED, isExtended);

        // Render the Base (Now just the stone part if progress > 0)
        renderBlock3D(guiGraphics, baseState, gridX, gridY, gridZ);

        // Render the Moving Head
        if (isExtended) {
            BlockState headState = Blocks.PISTON_HEAD.defaultBlockState()
                    .setValue(PistonHeadBlock.FACING, direction)
                    // Short = true makes the 'arm' of the piston head shorter (used for the base connection)
                    .setValue(PistonHeadBlock.SHORT, false);

            // Calculate movement offset
            float offX = direction.getStepX() * progress;
            float offY = direction.getStepY() * progress;
            float offZ = direction.getStepZ() * progress;

            renderBlock3D(guiGraphics, headState, gridX + offX, gridY + offY, gridZ + offZ);
        }
    }

    /**
     * The "Master Hook". This prepares the 3D space and then runs the developer's custom code.
     * Use this for things Prism doesn't support yet, like custom Mobs, Particles, or complex Machines.
     */
    public static void renderCustom3D(GuiGraphics guiGraphics, float gridX, float gridY, float gridZ, IPrismCustomRenderer renderer) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // Move to the grid position and center the "anchor point"
        poseStack.translate(gridX - 0.5f, gridY - 0.5f, gridZ - 0.5f);

        // Execute the developer's custom code
        // We provide the poseStack so they can do their own rotations/scaling inside their block space
        renderer.render(guiGraphics, poseStack, guiGraphics.bufferSource(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));

        poseStack.popPose();
    }

    /**
     * Renders a 3D block-breaking "crack" overlay at a specific grid position within the 3D camera context.
     * <p>
     * This method uses the vanilla {@link BlockRenderDispatcher} to map the crumbling texture
     * perfectly onto the geometry of the provided {@link BlockState}. This ensures that
     * complex shapes like stairs, slabs, or walls show cracks that follow their actual 3D model.
     * <p>
     * <b>Requirements:</b> This must be called between {@link #enableCamera} and {@link #disableCamera}.
     *
     * @param guiGraphics The current GuiGraphics instance.
     * @param state       The BlockState whose shape the cracks should follow (e.g., {@code Blocks.STONE.defaultBlockState()}).
     * @param stage       The destruction stage, ranging from 0 (light cracks) to 9 (near broken).
     * Values outside 0-9 will result in no rendering.
     * @param gridX       The relative X-coordinate in the 3D grid.
     * @param gridY       The relative Y-coordinate in the 3D grid.
     * @param gridZ       The relative Z-coordinate in the 3D grid.
     * @see #enableCamera(GuiGraphics, int, int, float, float, float)
     * @see #renderBlock3D(GuiGraphics, BlockState, float, float, float)
     */
    public static void renderBreakingTexture3D(GuiGraphics guiGraphics, BlockState state, int stage, float gridX, float gridY, float gridZ) {
        if (stage < 0 || stage > 9) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // 1. Position with a tiny Z-offset to prevent Z-fighting
        poseStack.translate(gridX - 0.5f, gridY - 0.5f, gridZ - 0.495f);
        poseStack.scale(1.001f, 1.001f, 1.001f); // Tiny scale up to wrap the block

        // 2. THE SECRET SAUCE: SheetedDecalTextureGenerator
        // This takes the standard block model and "re-paints" the breaking texture over it.
        MultiBufferSource.BufferSource bufferSource = guiGraphics.bufferSource();

        // Get the texture atlas for the crumbling blocks
        var crumblingTex = ModelBakery.DESTROY_TYPES.get(stage);

        // We create a special consumer that redirects the block's vertices
        // to use the crumbling texture instead of its normal texture.
        VertexConsumer crackingConsumer = new SheetedDecalTextureGenerator(
                bufferSource.getBuffer(crumblingTex),
                poseStack.last(),
                1.0f // Scale of the decal
        );

        // 3. Render the block model using our Cracking Consumer
        // We use the ModelRenderer directly here for maximum control
        mc.getBlockRenderer().getModelRenderer().tesselateBlock(
                mc.level,
                mc.getBlockRenderer().getBlockModel(state),
                state,
                BlockPos.ZERO,
                poseStack,
                crackingConsumer,
                false, // Don't use checkSides (we want to see all cracks)
                mc.level.random,
                state.getSeed(BlockPos.ZERO),
                OverlayTexture.NO_OVERLAY
        );

        // 4. Force the draw call immediately
        guiGraphics.flush();

        poseStack.popPose();
    }
}