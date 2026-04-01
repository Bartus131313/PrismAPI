package com.bartekkansy.prism.api.client.render;

import com.bartekkansy.prism.api.client.ui.PrismAnimation;
import com.bartekkansy.prism.api.client.ui.PrismDirection;
import com.bartekkansy.prism.api.fluid.FluidTankInfo;
import com.bartekkansy.prism.api.fluid.IPrismFluidHelper;
import com.bartekkansy.prism.api.util.PrismNumberFormatter;
import com.bartekkansy.prism.api.util.PrismPlatform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
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

        // Calculate the scaling
        float capacityRatio = (float) amount / maxAmount;
        int fluidHeight = (int) (height * capacityRatio);

        // Adjust Y so it draws from the bottom of the tank up
        int fluidY = y + (height - fluidHeight);

        // Call existing fluid rendering function cuz why not
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
        // Draw the fluid tank first
        renderFluidTank(guiGraphics, fluid, amount, maxAmount, x, y, width, height);

        // Get the custom list of components
        FluidTankInfo info = new FluidTankInfo(fluid, amount, maxAmount);
        List<Component> customTooltip = tooltipProvider.apply(info);

        // And render container tooltip with existing function
        renderContainerTooltip(guiGraphics, x, y, width, height, mouseX, mouseY, customTooltip);
    }

    /**
     * Renders a fluid tank with a standard "Name: Amount / Capacity mB" tooltip.
     * <p>
     * This is a convenience method for common use cases where custom formatting
     * is not required.
     * </p>
     *
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

        // Call function to render the fluid tank with tooltip
        PrismRenderer.renderFluidTankWithTooltip(guiGraphics, fluid, amount, maxAmount, x, y, width, height, mouseX, mouseY, (info) -> {
            List<Component> lines = new ArrayList<>();

            // Create the default tooltip text
            lines.add(Component.empty().append(info.getDisplayName())
                    .append(Component.literal(": " + PrismNumberFormatter.format(amount) + " / " + PrismNumberFormatter.format(maxAmount) + " mB"))
                    .withStyle(ChatFormatting.GRAY));

            // Return lines as List
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
        // Check if mouse is inside the box
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            // Render default Minecraft tooltip
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

        // Get bottom and top coords
        int fillHeight = (int) (height * fillAmount);
        int top = y + (height - fillHeight);
        int bottom = y + height;

        // Fill with gradient
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

        // Get right coords
        int fillWidth = (int) (width * fillAmount);
        int right = x + fillWidth;

        // Fill with gradient
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

        // Calculate the start point of the filled part
        int screenY = y + (height - fillHeight);
        int textureV = v + (height - fillHeight);

        // Blit the texture on the screen
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

        // Calculate fill amount
        int fillWidth = (int) (width * fillAmount);

        // Blit on the screen
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
    public static void renderStringGradient(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, int colorStart, int colorEnd, boolean shadow) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);

        // Get total width of the text
        float totalWidth = font.width(text);
        final float[] currentX = {0};

        // Get correct colors as int
        int r0 = (colorStart >> 16) & 0xFF, g0 = (colorStart >> 8) & 0xFF, b0 = colorStart & 0xFF, a0 = (colorStart >> 24) & 0xFF;
        int r1 = (colorEnd >> 16) & 0xFF, g1 = (colorEnd >> 8) & 0xFF, b1 = colorEnd & 0xFF, a1 = (colorEnd >> 24) & 0xFF;

        // Render each char separately
        text.getVisualOrderText().accept((index, style, codePoint) -> {
            // Get current char
            String ch = new String(Character.toChars(codePoint));
            float delta = currentX[0] / Math.max(1, totalWidth);

            // Interpolate Gradient
            int gr = (int) PrismAnimation.lerp(r0, r1, delta);
            int gg = (int) PrismAnimation.lerp(g0, g1, delta);
            int gb = (int) PrismAnimation.lerp(b0, b1, delta);
            int ga = (int) PrismAnimation.lerp(a0, a1, delta);

            // Component Tint
            int cCol = style.getColor() != null ? style.getColor().getValue() : 0xFFFFFF;

            // Multiply channels
            int fr = (gr * ((cCol >> 16) & 0xFF)) / 255;
            int fg = (gg * ((cCol >> 8) & 0xFF)) / 255;
            int fb = (gb * (cCol & 0xFF)) / 255;

            // Draw string
            guiGraphics.drawString(font, ch, (int)currentX[0], 0, (ga << 24) | (fr << 16) | (fg << 8) | fb, shadow);

            // Add X pos to current pos
            currentX[0] += font.width(FormattedCharSequence.forward(ch, style));
            return true;
        });

        // Pop Pose after rendering
        guiGraphics.pose().popPose();
    }

    /**
     * Renders a gradient colored string using a {@link Color} objects.
     *
     * @see #renderStringGradient(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderStringGradient(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, Color colorStart, Color colorEnd, boolean shadow) {
        renderStringGradient(guiGraphics, font, text, x, y, scale, colorStart.getRGB(), colorEnd.getRGB(), shadow);
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
     * @see #renderStringGradient(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderStringGradientCenteredX(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, int colorStart, int colorEnd, boolean shadow) {
        float width = font.width(text) * scale;
        renderStringGradient(guiGraphics, font, text, (int)(x - width / 2), y, scale, colorStart, colorEnd, shadow);
    }

    /**
     * Renders a string centered horizontally using a gradient with {@link Color} objects.
     *
     * @see #renderStringGradientCenteredX(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderStringGradientCenteredX(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, Color colorStart, Color colorEnd, boolean shadow) {
        renderStringGradientCenteredX(guiGraphics, font, text, x, y, scale, colorStart.getRGB(), colorEnd.getRGB(), shadow);
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
     * @see #renderStringGradient(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderStringGradientCenteredY(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, int colorStart, int colorEnd, boolean shadow) {
        float height = font.lineHeight * scale;
        renderStringGradient(guiGraphics, font, text, x, (int)(y - height / 2), scale, colorStart, colorEnd, shadow);
    }

    /**
     * Renders a string centered vertically using a gradient with {@link Color} objects.
     *
     * @see #renderStringGradientCenteredY(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderStringGradientCenteredY(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, Color colorStart, Color colorEnd, boolean shadow) {
        renderStringGradientCenteredY(guiGraphics, font, text, x, y, scale, colorStart.getRGB(), colorEnd.getRGB(), shadow);
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
     * @see #renderStringGradient(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderStringGradientCenteredXY(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, int colorStart, int colorEnd, boolean shadow) {
        float width = font.width(text) * scale;
        float height = font.lineHeight * scale;
        renderStringGradient(guiGraphics, font, text, (int)(x - width / 2), (int)(y - height / 2), scale, colorStart, colorEnd, shadow);
    }

    /**
     * Renders a string centered vertically and horizontally using a gradient with {@link Color} objects.
     *
     * @see #renderStringGradientCenteredXY(GuiGraphics, Font, Component, int, int, float, int, int, boolean)
     */
    public static void renderStringGradientCenteredXY(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale, Color colorStart, Color colorEnd, boolean shadow) {
        renderStringGradientCenteredXY(guiGraphics, font, text, x, y, scale, colorStart.getRGB(), colorEnd.getRGB(), shadow);
    }
}