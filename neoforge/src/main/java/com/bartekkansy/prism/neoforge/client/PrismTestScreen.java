package com.bartekkansy.prism.neoforge.client;

import com.bartekkansy.prism.api.client.render.PrismRenderer;
import com.bartekkansy.prism.api.client.ui.PrismAnimation;
import com.bartekkansy.prism.api.client.ui.PrismDirection;
import com.bartekkansy.prism.api.client.ui.PrismLayout;
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

    // --- API COMPONENTS ---
    private final PrismLayout centralDashboard = new PrismLayout();
    private final PrismLayout sidebarNodes = new PrismLayout();

    // --- ANIMATION STATE ---
    private float smoothProgress = 0.0f;
    private float targetProgress = 0.0f;
    private long lastMs = System.currentTimeMillis();

    // --- ASSETS ---
    private static final ResourceLocation FURNACE_GUI = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/furnace.png");

    public PrismTestScreen() {
        super(Component.literal("Prism API Master Dashboard"));

        // 1. [LAYOUT] CENTRAL DASHBOARD (Horizontal)
        centralDashboard.setSpacing(20);

        // Feature: Fluid Tank with Custom Data Provider
        centralDashboard.addElement(34, 100, (info) -> {
            float level = PrismAnimation.easeInOutQuad(smoothProgress);
            PrismRenderer.renderFluidTankWithTooltip(info.guiGraphics(), Fluids.LAVA, (int)(2000 * level), 2000,
                    info.x(), info.y(), info.width(), info.height(), info.mouseX(), info.mouseY(),
                    (tank) -> List.of(
                            Component.literal("THERMAL REACTOR").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                            Component.literal("Capacity: " + tank.amount() + " / " + tank.capacity() + " mB").withStyle(ChatFormatting.GRAY),
                            Component.literal("Efficiency: " + (int)(smoothProgress * 100) + "%").withStyle(ChatFormatting.GOLD)
                    ));
        });

        // Feature: Scaled Typography & Dynamic Logic
        centralDashboard.addElement(120, 100, (info) -> {
            info.guiGraphics().fill(info.x(), info.y(), info.x() + info.width(), info.y() + info.height(), 0x22FFFFFF);

            PrismRenderer.renderStringCenteredX(info.guiGraphics(), font, info.x() + (info.width()/2), info.y() + 10, 1.2f,
                    Component.literal("SYSTEM STATUS").withStyle(ChatFormatting.UNDERLINE), Color.WHITE, true);

            String status = smoothProgress > 0.8f ? "OVERLOAD" : (smoothProgress < 0.2f ? "IDLE" : "ACTIVE");
            Color color = smoothProgress > 0.8f ? Color.RED : (smoothProgress < 0.2f ? Color.GRAY : Color.GREEN);

            // Using easeOutBack for a "popping" text effect
            float textScale = 1.0f + (PrismAnimation.easeInOutQuad(smoothProgress) * 0.5f);
            PrismRenderer.renderStringCenteredXY(info.guiGraphics(), font, info.x() + (info.width()/2), info.y() + 50,
                    textScale, Component.literal(status), color, true);
        });

        // 2. [LAYOUT] SIDEBAR NODES (Vertical)
        sidebarNodes.setSpacing(10);
        for(int i = 1; i <= 3; i++) {
            final int id = i;
            sidebarNodes.addElement(50, 25, (info) -> {
                // Feature: Horizontal Directional Progress
                PrismRenderer.renderProgressBar(info.guiGraphics(), info.x(), info.y(), info.width(), info.height(),
                        smoothProgress, new Color(100, 0, 255), new Color(40, 0, 100), PrismDirection.RIGHT);

                PrismRenderer.renderStringCenteredXY(info.guiGraphics(), font, info.x() + 25, info.y() + 12, 0.7f,
                        Component.literal("NODE-" + id), Color.WHITE, false);
            });
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // --- 1. ANIMATION LOGIC ---
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastMs) / 1000f;
        lastMs = now;

        // Toggle target every 3 seconds for demo purposes
        this.targetProgress = (now % 6000 < 3000) ? 1.0f : 0.0f;
        // Smoothly interpolate the progress value
        this.smoothProgress = PrismAnimation.lerp(smoothProgress, targetProgress, deltaTime * 3.5f);

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // --- 2. HEADER ---
        PrismRenderer.renderStringCenteredX(guiGraphics, font, width / 2, 20, 2.5f,
                Component.literal("PRISM FRAMEWORK").withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE), Color.WHITE, true);

        // --- 3. DRAW LAYOUTS ---
        int dashboardX = (width / 2) - (centralDashboard.getTotalWidth() / 2);
        int dashboardY = (height / 2) - 50;
        centralDashboard.drawHorizontal(guiGraphics, dashboardX, dashboardY, mouseX, mouseY);

        sidebarNodes.drawVertical(guiGraphics, 20, 60, mouseX, mouseY);

        // --- 4. DIRECTIONAL STRESS TEST (Grid) ---
        int gridX = 20;
        int gridY = height - 80;
        // Testing UP, DOWN, LEFT, RIGHT in a single cluster
        PrismRenderer.renderProgressBar(guiGraphics, gridX, gridY, 10, 40, smoothProgress, Color.YELLOW, PrismDirection.UP);
        PrismRenderer.renderProgressBar(guiGraphics, gridX + 15, gridY, 10, 40, smoothProgress, Color.ORANGE, PrismDirection.DOWN);
        PrismRenderer.renderProgressBar(guiGraphics, gridX + 30, gridY + 10, 40, 10, smoothProgress, Color.RED, PrismDirection.LEFT);
        PrismRenderer.renderProgressBar(guiGraphics, gridX + 30, gridY + 25, 40, 10, smoothProgress, Color.PINK, PrismDirection.RIGHT);

        // --- 5. TEXTURED PROGRESS (UV CLIPPING) ---
        int texX = width - 60;
        int texY = 60;
        // Feature: Textured Progress with automatic UV calculation
        PrismRenderer.renderProgressBarTexture(guiGraphics, FURNACE_GUI, texX, texY, 24, 17, smoothProgress, 79, 34, PrismDirection.RIGHT);
        PrismRenderer.renderStringCenteredX(guiGraphics, font, texX + 12, texY + 22, 0.6f, Component.literal("UV_CLIP"), Color.GRAY, false);

        // --- 6. SCISSOR MARQUEE (Clipped Area) ---
        int mWidth = 180;
        int mX = width - mWidth - 20;
        int mY = height - 40;
        guiGraphics.fill(mX - 2, mY - 2, mX + mWidth + 2, mY + 14, 0x55000000);

        PrismRenderer.startScissor(guiGraphics, mX, mY, mWidth, 12);
        // Animate text offset based on smoothProgress
        int scroll = (int)(smoothProgress * 400) - 150;
        PrismRenderer.renderString(guiGraphics, font, mX + scroll, mY + 2, 1.0f,
                Component.literal("SCISSOR CLIPPING ENABLED • LERPING ACTIVE • RENDER ENGINE STABLE"), Color.GREEN, false);
        PrismRenderer.stopScissor(guiGraphics);

        // --- 7. THE REQUESTED WATERMARK (Bottom Left) ---
        PrismRenderer.renderString(guiGraphics, font, 10, height - 15, 0.8f,
                Component.literal("Prism API v1.0.0").withStyle(ChatFormatting.ITALIC), Color.LIGHT_GRAY, false);

        // Final Watermark details
        PrismRenderer.renderString(guiGraphics, font, 10, height - 25, 0.5f,
                Component.literal("DEVELOPER BUILD - BK"), Color.GRAY, false);
    }
}