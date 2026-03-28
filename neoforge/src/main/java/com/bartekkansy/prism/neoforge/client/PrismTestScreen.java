package com.bartekkansy.prism.neoforge.client;

import com.bartekkansy.prism.api.client.render.PrismRenderer;
import com.bartekkansy.prism.api.client.ui.PrismDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluids;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PrismTestScreen extends Screen {

    public PrismTestScreen() {
        super(Component.literal("Prism API Debug Screen"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw a dark background so we can see the fluid
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        PrismRenderer.renderFluidTankWithTooltip(guiGraphics, Fluids.LAVA, 500, 1000, 100, 100, 32, 128, mouseX, mouseY,
                (info) -> {
                    List<Component> lines = new ArrayList<>();

                    lines.add(Component.translatable("prism.test.container_info", info.amount(), info.getDisplayName())
                            .withStyle(ChatFormatting.GOLD));

                    lines.add(Component.translatable("prism.test.capacity", info.capacity())
                            .withStyle(ChatFormatting.DARK_GRAY));

                    return lines;
                }
        );

        PrismRenderer.renderVerticalFillContainer(guiGraphics, 200, 100, 32, 128, (float)1024 / (float)4096, Color.CYAN, Color.BLUE);

        PrismRenderer.renderHorizontalFillContainer(guiGraphics, 300, 100, 128, 32, (float)2048 / (float)4096, Color.ORANGE, Color.RED);

        PrismRenderer.renderProgressBar(guiGraphics, 400, 100, 32, 128, 0.75f, Color.RED, Color.GREEN, PrismDirection.UP);
    }
}