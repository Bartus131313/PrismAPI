package com.bartekkansy.test;

import com.bartekkansy.prism.api.client.render.PrismRenderer;
import com.bartekkansy.prism.api.client.ui.PrismAnimation;
import com.bartekkansy.prism.api.client.ui.PrismDirection;
import com.bartekkansy.prism.api.client.ui.PrismLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

public class TestScreen extends Screen {

    // --- API COMPONENTS ---
    private final PrismLayout centralDashboard = new PrismLayout();
    private final PrismLayout sidebarNodes = new PrismLayout();

    // --- ANIMATION STATE ---
    private float smoothProgress = 0.0f;
    private float targetProgress = 0.0f;
    private long lastMs = System.currentTimeMillis();

    // --- ASSETS ---
    private static final ResourceLocation FURNACE_GUI = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/furnace.png");

    public TestScreen() {
        super(Component.literal("Prism API Master Dashboard"));

        // 1. [LAYOUT] CENTRAL DASHBOARD (Horizontal)
        centralDashboard.setSpacing(20);

        // Feature: Fluid Tank with Custom Data Provider
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

        // Feature: Scaled Typography & Dynamic Logic
        centralDashboard.addElement(120, 100, (info) -> {
            info.guiGraphics().fill(info.x(), info.y(), info.x() + info.width(), info.y() + info.height(), 0x22FFFFFF);

            PrismRenderer.renderStringCenteredX(info.guiGraphics(), font, Component.literal("SYSTEM STATUS").withStyle(ChatFormatting.UNDERLINE),
                    info.x() + (info.width()/2), info.y() + 10, 1.2f, Color.WHITE, true);

            String status = smoothProgress > 0.8f ? "OVERLOAD" : (smoothProgress < 0.2f ? "IDLE" : "ACTIVE");
            Color color = smoothProgress > 0.8f ? Color.RED : (smoothProgress < 0.2f ? Color.GRAY : Color.GREEN);

            // Using easeOutBack for a "popping" text effect
            float textScale = 1.0f + (PrismAnimation.easeInOutQuart(smoothProgress) * 0.5f);
            PrismRenderer.renderStringCenteredXY(info.guiGraphics(), font, Component.literal(status),
                    info.x() + (info.width()/2), info.y() + 50, textScale, color, true);
        });

        // 2. [LAYOUT] SIDEBAR NODES (Vertical)
        sidebarNodes.setSpacing(10);
        for(int i = 1; i <= 3; i++) {
            final int id = i;
            sidebarNodes.addElement(50, 25, (info) -> {
                // Feature: Horizontal Directional Progress
                PrismRenderer.renderProgressBar(info.guiGraphics(), info.x(), info.y(), info.width(), info.height(),
                        smoothProgress, new Color(100, 0, 255), new Color(40, 0, 100), PrismDirection.RIGHT);

                PrismRenderer.renderStringCenteredXY(info.guiGraphics(), font, Component.literal("NODE-" + id),
                        info.x() + 25, info.y() + 12, 0.7f, Color.WHITE, false);
            });
        }
    }

    private static final ChestBlockEntity DUMMY_CHEST = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // --- 1. ANIMATION LOGIC ---
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastMs) / 1000f;
        lastMs = now;

        // Toggle target every 3 seconds for demo purposes
        // this.targetProgress = (now % 6000 < 3000) ? 1.0f : 0.0f;
        this.targetProgress = (now % 6000 < 3000) ? (float) (now % 3000) / 3000 : 1f - (float) (now % 3000) / 3000;
        this.smoothProgress = PrismAnimation.easeInOutCubic(this.targetProgress);
        // Smoothly interpolate the progress value
        // this.smoothProgress = PrismAnimation.lerp(smoothProgress, targetProgress, deltaTime * 3.5f);

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // --- 2. HEADER ---
        PrismRenderer.renderGradientStringCenteredX(guiGraphics, font, Component.literal("PRISM FRAMEWORK").withStyle(ChatFormatting.BOLD),
                width / 2, 20, 2.5f, Color.RED, Color.BLUE, true);

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
        int texX = width - 100;
        int texY = 60;
        // Feature: Textured Progress with automatic UV calculation
        PrismRenderer.renderProgressBarTexture(guiGraphics, FURNACE_GUI, texX, texY, 24, 17, smoothProgress, 79, 34, PrismDirection.RIGHT);
        PrismRenderer.renderStringCenteredX(guiGraphics, font, Component.literal("UV_CLIP"), texX + 12, texY + 22, 0.6f, Color.GRAY, false);

        // --- 6. SCISSOR MARQUEE (Clipped Area) ---
        int mWidth = 180;
        int mX = width - mWidth - 20;
        int mY = height - 40;
        guiGraphics.fill(mX - 2, mY - 2, mX + mWidth + 2, mY + 14, 0x55000000);

        PrismRenderer.startScissor(guiGraphics, mX, mY, mWidth, 12);

        String text = "SCISSOR CLIPPING ENABLED • LERPING ACTIVE • RENDER ENGINE STABLE";

        // Logic: Text slides from left to right based on smoothProgress
        int scroll = (int) (smoothProgress * (mWidth - font.width(text)));

        // Now using the [Component -> Coordinates] signature
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

        // --- 7. THE REQUESTED WATERMARK (Bottom Left) ---
        PrismRenderer.renderString(guiGraphics, font, Component.literal("Prism API v1.0.1").withStyle(ChatFormatting.ITALIC),
                10, height - 15, 0.8f, Color.LIGHT_GRAY, false);

        // Final Watermark details
        PrismRenderer.renderString(guiGraphics, font, Component.literal("DEVELOPER BUILD - BK"),
                10, height - 25, 0.5f, Color.GRAY, false);

        // Turn on the 3D Engine at screen coordinates, Scale 2.0x, Isometric View
        PrismRenderer.enableCamera(guiGraphics, 550, 180, 1.0f, 30f, 225f + 360f * smoothProgress);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y < 3; y++) {
                    // Layer 0: 3x3 Gold Base
                    if (y == 0) {
                        PrismRenderer.renderBlock3D(guiGraphics, Blocks.GOLD_BLOCK, x, y, z);
                        if (x % 2 != 0 && z % 2 != 0)
                            PrismRenderer.renderBreakingTexture3D(guiGraphics, Blocks.GOLD_BLOCK.defaultBlockState(), (int)(9 * smoothProgress), x, y, z);
                    }

                    // Layer 1: Center Core
                    else if (x == 0 && y == 1 && z == 0) {
                        PrismRenderer.renderBlock3D(guiGraphics, Blocks.NETHERRACK, x, y, z);
                    }

                    // Layer 1: The "Cross" of Redstone Torches
                    // Condition: (Abs X + Abs Z == 1) ensures it's North, South, East, or West of center
                    else if (y == 1 && (Math.abs(x) + Math.abs(z) == 1)) {
                        PrismRenderer.renderBlock3D(guiGraphics, Blocks.REDSTONE_TORCH, x, y, z);
                    }

                    // Layer 2: Fire on top of the Core
                    else if (x == 0 && y == 2 && z == 0) {
                        PrismRenderer.renderBlock3D(guiGraphics, Blocks.FIRE, x, y, z);
                    }
                }
            }
        }

        // Turn off the engine and flush the renders
        PrismRenderer.disableCamera(guiGraphics);

        // Draw normal 2D GUI stuff over it
        PrismRenderer.renderGradientStringCenteredXY(guiGraphics, font, Component.literal("Herobrine Altar"), 550, 220, 1f, Color.GREEN, Color.BLUE, false);

        PrismRenderer.enableCamera(guiGraphics, 150, 170, 1.5f, 30f, 225f);

        PrismRenderer.renderPiston3D(guiGraphics, Direction.UP, smoothProgress, 0, 0, 0);

        PrismRenderer.renderCustom3D(guiGraphics, 1, 0, 0, (graphics, poseStack, bufferSource, pt) -> {
            Minecraft mc = Minecraft.getInstance();

            // Ensure the dummy chest knows about the world (needed for textures)
            if (DUMMY_CHEST.getLevel() == null) DUMMY_CHEST.setLevel(mc.level);

            // Get the vanilla Chest Renderer
            var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(DUMMY_CHEST);

            if (renderer != null) {
                // We can actually use the 'partialTick' parameter of the hook
                // OR pass our custom openProgress directly if the renderer supports it.
                // Most Chest renderers use the internal 'lidness' of the entity:
                // DUMMY_CHEST.openness = openProgress;

                renderer.render(DUMMY_CHEST, pt, poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
            }
        });

        PrismRenderer.disableCamera(guiGraphics);
    }
}