package com.bartekkansy.test.screen;

import com.bartekkansy.prism.api.client.render.world.PrismCamera;
import com.bartekkansy.prism.api.client.render.PrismRenderer;
import com.bartekkansy.prism.api.client.render.world.PrismVirtualSpace;
import com.bartekkansy.prism.api.client.ui.animation.PrismAnimation;
import com.bartekkansy.prism.api.client.ui.animation.PrismAnimator;
import com.bartekkansy.test.util.Utilities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TestScreen extends Screen {

    // Animation space
    private float smoothProgress = 0.0f;
    private float targetProgress = 0.0f;

    // Virtual space things
    private final PrismCamera renderCamera = PrismCamera.ISO_VIEW.copy();
    private final PrismVirtualSpace mySpace = new PrismVirtualSpace();

    // Create dummy chest for future rendering
    private static final ChestBlockEntity DUMMY_CHEST = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());

    public TestScreen() {
        super(Component.literal("Prism API - Test Screen"));

        // A Stone Wall Corner (Shows Wall connections)
        BlockState wall = Blocks.COBBLESTONE_WALL.defaultBlockState();
        mySpace.putBlock(new BlockPos(-1, 0, -1), wall);
        mySpace.putBlock(new BlockPos(-1, 0,  0), wall);
        mySpace.putBlock(new BlockPos( 0, 0, -1), wall);

        // A Cozy Wooden Bench (Shows Stair connections)
        BlockState stair = Blocks.OAK_STAIRS.defaultBlockState();
        mySpace.putBlock(new BlockPos(1, 0, 1), stair.setValue(StairBlock.FACING, Direction.NORTH));
        mySpace.putBlock(new BlockPos(0, 0, 1), stair.setValue(StairBlock.FACING, Direction.EAST));

        // Central Decoration
        mySpace.putBlock(new BlockPos(0, 0, 0), Blocks.COAL_BLOCK.defaultBlockState());
        mySpace.putBlock(new BlockPos(0, 1, 0), Blocks.FURNACE.defaultBlockState()
                .setValue(FurnaceBlock.LIT, true)
                .setValue(FurnaceBlock.FACING, Direction.EAST));

        // Glass Display (Shows Pane connections)
        BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
        mySpace.putBlock(new BlockPos(1, 0, -1), pane);
        mySpace.putBlock(new BlockPos(0, 0, -1), pane); // This connects to the wall AND the other pane!

        // Lantern
        mySpace.putBlock(new BlockPos(-1, 1, -1), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, false));
    }

    private final PrismAnimation testAnim = new PrismAnimation(1f, PrismAnimator::easeInOutCubic);

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Get current time in ms
        long now = System.currentTimeMillis();

        // Get cool animation progress
        this.targetProgress = (now % 6000 < 3000) ? (float) (now % 3000) / 3000 : 1f - (float) (now % 3000) / 3000;
        this.smoothProgress = PrismAnimator.easeInOutCubic(this.targetProgress);

        // Redner background - blur
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Header
        PrismRenderer.renderGradientStringCenteredX(guiGraphics, font, Component.literal("Prism API - Test Screen").withStyle(ChatFormatting.BOLD),
                width / 2, 20, 2.5f, Color.RED, Color.BLUE, true);

        // Render Herobrine Altar
        PrismRenderer.renderGradientStringCenteredXY(guiGraphics, font, Component.literal("Herobrine Altar"), width / 2, height / 2 + 60, 0.8f, Color.RED, Color.ORANGE, true);
        renderCamera.setRotX(30f).setRotY(225f + 360f * smoothProgress).setRotZ(0f).setZoom(1.5f).enable(guiGraphics, width / 2, height / 2);

        // Render Herobrine Altar using raw block rendering funcs instead of virtual space
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y < 3; y++) {
                    // Gold base
                    if (y == 0) {
                        PrismRenderer.renderBlock3D(guiGraphics, Blocks.GOLD_BLOCK, x, y, z);
                        if (x % 2 != 0 && z % 2 != 0)
                            PrismRenderer.renderBreakingTexture3D(guiGraphics, Blocks.GOLD_BLOCK.defaultBlockState(), (int)(9 * smoothProgress), x, y, z);
                    }

                    // Netherrack at the center
                    else if (x == 0 && y == 1 && z == 0) {
                        PrismRenderer.renderBlock3D(guiGraphics, Blocks.NETHERRACK, x, y, z);
                    }

                    // Redstone torches
                    else if (y == 1 && (Math.abs(x) + Math.abs(z) == 1)) {
                        PrismRenderer.renderBlock3D(guiGraphics, Blocks.REDSTONE_TORCH, x, y, z);
                    }

                    // Fire at the top
                    else if (x == 0 && y == 2 && z == 0) {
                        PrismRenderer.renderBlock3D(guiGraphics, Blocks.FIRE, x, y, z);
                    }
                }
            }
        }

        // Turn off the engine and flush the renders
        renderCamera.disable(guiGraphics);

        PrismRenderer.renderGradientStringCenteredXY(guiGraphics, font, Component.literal("Piston test"), width / 4, height / 2 + 60, 0.8f, Color.BLUE, Color.MAGENTA, true);
        renderCamera.setRotX(30f).setRotY(225f).setRotZ(0f).setZoom(1.5f).enable(guiGraphics, width / 4, height / 2);

        PrismRenderer.renderPiston3D(guiGraphics, Direction.UP, smoothProgress, 0, 0, 0);

        PrismRenderer.renderCustom3D(guiGraphics, 1, 0, 0, (graphics, poseStack, bufferSource, pt) -> {
            Minecraft mc = Minecraft.getInstance();

            if (DUMMY_CHEST.getLevel() == null) DUMMY_CHEST.setLevel(mc.level);

            // Get the vanilla Chest Renderer
            var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(DUMMY_CHEST);

            if (renderer != null) {
                // TODO: Opening the chest.

                renderer.render(DUMMY_CHEST, pt, poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
            }
        });

        renderCamera.disable(guiGraphics);

        // Complex scene
        PrismRenderer.renderGradientStringCenteredXY(guiGraphics, font, Component.literal("Complex Geometry Showcase"),
                (width / 4) * 3, height / 2 + 60, 0.8f, Color.ORANGE, Color.YELLOW, true);

        renderCamera.setRotX(30f)
                .setRotY(225f - 360f * smoothProgress)
                .setRotZ(0f)
                .setZoom(1.5f + 0.5f * smoothProgress)
                .enable(guiGraphics, (width / 4) * 3, height / 2);

        PrismRenderer.renderSpace(guiGraphics, mySpace);

        renderCamera.disable(guiGraphics);

        testAnim.render(guiGraphics, animationInfo -> {
            PrismRenderer.renderStringCenteredXY(animationInfo.guiGraphics(), font, Component.literal("TEST"), width / 2, height - 50,
                    animationInfo.progress() + 1f, Color.BLUE, true);
        });

        // Render watermark at the end
        Utilities.renderWatermark(guiGraphics, width, height);
    }
}