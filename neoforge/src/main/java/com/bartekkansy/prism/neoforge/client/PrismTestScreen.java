package com.bartekkansy.prism.neoforge.client;

import com.bartekkansy.prism.api.client.render.PrismRenderer;
import com.bartekkansy.prism.api.client.ui.PrismDirection;
import com.bartekkansy.prism.api.util.PrismNumberFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PrismTestScreen extends Screen {

    // Dummy texture for progress bar testing (using vanilla furnace arrow as example)
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/furnace.png");

    public PrismTestScreen() {
        super(Component.literal("Prism API Debug Screen"));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 1. Background
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // 2. Animated Progress (0.0 to 1.0 based on system time)
        float pulse = (System.currentTimeMillis() % 2000) / 2000f;

        // --- SECTION: TYPOGRAPHY ---
        PrismRenderer.renderStringCenteredX(guiGraphics, font, width / 2, 10, 1.5f,
                Component.literal("PRISM UI DEBUGGER").withStyle(ChatFormatting.BOLD), Color.CYAN, true);

        PrismRenderer.renderString(guiGraphics, font, 10, height - 20, 0.7f,
                Component.literal("Prism API v1.0 - Scaled Footer Text"), Color.GRAY, false);

        // --- SECTION: FLUID TANKS ---
        // Water Tank (Custom Tooltip)
        guiGraphics.fill(45, 45, 87, 178, 0xaa000000); // Background frame
        PrismRenderer.renderFluidTankWithTooltip(guiGraphics, Fluids.WATER, 750, 1000, 50, 50, 32, 128, mouseX, mouseY,
                (info) -> {
                    List<Component> lines = new ArrayList<>();
                    lines.add(info.getDisplayName().copy().withStyle(ChatFormatting.AQUA));
                    lines.add(Component.literal(PrismNumberFormatter.format(info.amount()) + " mB").withStyle(ChatFormatting.GRAY));
                    return lines;
                }
        );

        // Lava Tank (Default Tooltip)
        guiGraphics.fill(95, 45, 137, 178, 0xaa000000);
        PrismRenderer.renderFluidTankWithTooltip(guiGraphics, Fluids.LAVA, (int)(1000 * pulse), 1000, 100, 50, 32, 128, mouseX, mouseY);

        // --- SECTION: PROCEDURAL PROGRESS BARS ---
        // Vertical Power Bar (Gradient)
        PrismRenderer.renderProgressBar(guiGraphics, 150, 50, 15, 128, pulse,
                new Color(0, 255, 0), new Color(0, 100, 0), PrismDirection.UP);
        PrismRenderer.renderContainerTooltip(guiGraphics, 150, 50, 15, 128, mouseX, mouseY, Component.literal("Energy: " + (int)(pulse * 100) + "%"));

        // Horizontal Health Bar (Solid Color)
        PrismRenderer.renderProgressBar(guiGraphics, 180, 50, 100, 10, pulse, Color.RED, PrismDirection.RIGHT);

        // Reverse Horizontal Bar (Right to Left)
        PrismRenderer.renderProgressBar(guiGraphics, 180, 65, 100, 10, pulse, Color.MAGENTA, PrismDirection.LEFT);

        // --- SECTION: TEXTURED PROGRESS ---
        // furnace arrow example (Horizontal)
        PrismRenderer.renderProgressBarTexture(guiGraphics, GUI_TEXTURE, 180, 85, 24, 17, pulse, 79, 34, PrismDirection.RIGHT);

        // --- SECTION: SCISSOR CLIPPING ---
        PrismRenderer.renderString(guiGraphics, font, 180, 110, 1.0f, Component.literal("Clipped Area:"), Color.YELLOW, true);

        // Start clipping to a small box
        PrismRenderer.startScissor(guiGraphics, 180, 125, 50, 20);
        guiGraphics.fill(180, 125, 230, 145, 0x44FFFFFF); // Debug box to see clipped area

        // This text is moving, but will only be visible inside the 50x20 box
        int xOffset = (int) (pulse * 100) - 50;
        PrismRenderer.renderString(guiGraphics, font, 180 + xOffset, 130, 1.0f, Component.literal("SCROLLING TEXT"), Color.WHITE, false);

        PrismRenderer.stopScissor(guiGraphics);

        // --- SECTION: CENTERED XY TEST ---
        // Perfect for "Loading..." text or item labels
        PrismRenderer.renderStringCenteredXY(guiGraphics, font, width - 60, height - 60, 1.0f,
                Component.literal("CENTERED"), Color.ORANGE, true);
    }
}