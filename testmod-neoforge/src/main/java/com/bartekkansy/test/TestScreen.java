package com.bartekkansy.test;

import com.bartekkansy.prism.api.client.render.PrismRenderer;
import com.bartekkansy.prism.api.client.ui.PrismAnimation;
import com.bartekkansy.prism.api.client.ui.PrismDirection;
import com.bartekkansy.prism.api.client.ui.PrismLayout;
import com.bartekkansy.test.util.Utilities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

public class TestScreen extends Screen {

    // Layouts
    private final PrismLayout centralDashboard = new PrismLayout();
    private final PrismLayout sidebarNodes = new PrismLayout();

    // Animation states
    private float smoothProgress = 0.0f;
    private float targetProgress = 0.0f;
    private long lastMs = System.currentTimeMillis();

    // Assets
    private static final ResourceLocation FURNACE_GUI = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/furnace.png");

    public TestScreen() {
        super(Component.literal("Prism API Master Dashboard"));

        centralDashboard.setSpacing(20);

        // Add fluid tank
        centralDashboard.addElement(34, 100, (info) -> {
            float level = PrismAnimation.easeInOutQuart(smoothProgress);
            PrismRenderer.renderFluidTankWithTooltip(info.guiGraphics(), Fluids.LAVA, (int)(2000 * level), 2000,
                    info.x(), info.y(), info.width(), info.height(), info.mouseX(), info.mouseY(),
                    (tank) -> List.of(
                            Component.literal("THERMAL REACTOR").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                            Component.literal("Capacity: " + tank.amount() + " / " + tank.capacity() + " mB").withStyle(ChatFormatting.GRAY),
                            Component.literal("Efficiency: " + (int)(smoothProgress * 100) + "%").withStyle(ChatFormatting.GOLD)
                    ));
        });

        // Add labels
        centralDashboard.addElement(120, 100, (info) -> {
            info.guiGraphics().fill(info.x(), info.y(), info.x() + info.width(), info.y() + info.height(), 0x22FFFFFF);

            PrismRenderer.renderStringCenteredX(info.guiGraphics(), font, Component.literal("SYSTEM STATUS").withStyle(ChatFormatting.UNDERLINE),
                    info.x() + (info.width()/2), info.y() + 10, 1.2f, Color.WHITE, true);

            String status = smoothProgress > 0.8f ? "OVERLOAD" : (smoothProgress < 0.2f ? "IDLE" : "ACTIVE");
            Color color = smoothProgress > 0.8f ? Color.RED : (smoothProgress < 0.2f ? Color.GRAY : Color.GREEN);

            float textScale = 1.0f + (PrismAnimation.easeInOutQuart(smoothProgress) * 0.5f);
            PrismRenderer.renderStringCenteredXY(info.guiGraphics(), font, Component.literal(status),
                    info.x() + (info.width()/2), info.y() + 50, textScale, color, true);
        });

        // Add sidebar nodes
        sidebarNodes.setSpacing(10);
        for(int i = 1; i <= 3; i++) {
            final int id = i;
            sidebarNodes.addElement(50, 25, (info) -> {
                PrismRenderer.renderProgressBar(info.guiGraphics(), info.x(), info.y(), info.width(), info.height(),
                        smoothProgress, new Color(100, 0, 255), new Color(40, 0, 100), PrismDirection.RIGHT);

                PrismRenderer.renderStringCenteredXY(info.guiGraphics(), font, Component.literal("NODE-" + id),
                        info.x() + 25, info.y() + 12, 0.7f, Color.WHITE, false);
            });
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Get current time and calculate delta
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastMs) / 1000f;
        lastMs = now;

        // Get cool animation progress
        this.targetProgress = (now % 6000 < 3000) ? 1.0f : 0.0f;
        this.smoothProgress = PrismAnimation.lerp(smoothProgress, targetProgress, deltaTime * 3.5f);

        // Render background blur
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Add header label
        PrismRenderer.renderStringGradientCenteredX(guiGraphics, font, Component.literal("PRISM FRAMEWORK").withStyle(ChatFormatting.BOLD),
                width / 2, 20, 2.5f, Color.RED, Color.BLUE, true);

        // Draw layouts
        int dashboardX = (width / 2) - (centralDashboard.getTotalWidth() / 2);
        int dashboardY = (height / 2) - 50;
        centralDashboard.drawHorizontal(guiGraphics, dashboardX, dashboardY, mouseX, mouseY);

        // Draw nodes
        sidebarNodes.drawVertical(guiGraphics, 20, 60, mouseX, mouseY);

        // Draw progress bars
        int gridX = 20;
        int gridY = height - 80;

        PrismRenderer.renderProgressBar(guiGraphics, gridX, gridY, 10, 40, smoothProgress, Color.YELLOW, PrismDirection.UP);
        PrismRenderer.renderProgressBar(guiGraphics, gridX + 15, gridY, 10, 40, smoothProgress, Color.ORANGE, PrismDirection.DOWN);
        PrismRenderer.renderProgressBar(guiGraphics, gridX + 30, gridY + 10, 40, 10, smoothProgress, Color.RED, PrismDirection.LEFT);
        PrismRenderer.renderProgressBar(guiGraphics, gridX + 30, gridY + 25, 40, 10, smoothProgress, Color.PINK, PrismDirection.RIGHT);

        // Add UV clipping
        int texX = width - 60;
        int texY = 60;

        PrismRenderer.renderProgressBarTexture(guiGraphics, FURNACE_GUI, texX, texY, 24, 17, smoothProgress, 79, 34, PrismDirection.RIGHT);
        PrismRenderer.renderStringCenteredX(guiGraphics, font, Component.literal("UV_CLIP"), texX + 12, texY + 22, 0.6f, Color.GRAY, false);

        // Add clipping area
        int mWidth = 180;
        int mX = width - mWidth - 20;
        int mY = height - 40;
        guiGraphics.fill(mX - 2, mY - 2, mX + mWidth + 2, mY + 14, 0x55000000);

        PrismRenderer.startScissor(guiGraphics, mX, mY, mWidth, 12);

        String text = "SCISSOR CLIPPING ENABLED • LERPING ACTIVE • RENDER ENGINE STABLE";
        int scroll = (int)(smoothProgress * (font.width(text) + 5)) - font.width(text);

        // Render scrolling label
        PrismRenderer.renderString(
                guiGraphics,
                font,
                Component.literal(text),
                mX + scroll,
                mY + 2,
                1.0f,
                Color.GREEN,
                false
        );

        PrismRenderer.stopScissor(guiGraphics);

        // Draw watermark
        Utilities.renderWatermark(guiGraphics, width, height);
    }
}