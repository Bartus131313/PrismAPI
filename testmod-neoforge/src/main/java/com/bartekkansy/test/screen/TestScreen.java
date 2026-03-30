package com.bartekkansy.test.screen;

import com.bartekkansy.prism.api.client.render.world.PrismCamera;
import com.bartekkansy.prism.api.client.render.PrismRenderer;
import com.bartekkansy.prism.api.client.render.world.PrismVirtualSpace;
import com.bartekkansy.prism.api.client.ui.PrismAnimation;
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

    // --- ANIMATION STATE ---
    private float smoothProgress = 0.0f;
    private float targetProgress = 0.0f;

    private final PrismVirtualSpace mySpace = new PrismVirtualSpace();

    public TestScreen() {
        super(Component.literal("Prism API - Test Screen"));

        // A Stone Wall Corner (Shows Wall connections)
        BlockState wall = Blocks.COBBLESTONE_WALL.defaultBlockState();
        mySpace.putBlock(new BlockPos(-1, 0, -1), wall);
        mySpace.putBlock(new BlockPos(-1, 0,  0), wall);
        mySpace.putBlock(new BlockPos( 0, 0, -1), wall);

        // A Cozy Wooden Bench (Shows Stair connections)
        // This will form an "L" shaped corner bench
        BlockState stair = Blocks.OAK_STAIRS.defaultBlockState();
        mySpace.putBlock(new BlockPos(1, 0, 1), stair.setValue(StairBlock.FACING, Direction.NORTH));
        mySpace.putBlock(new BlockPos(0, 0, 1), stair.setValue(StairBlock.FACING, Direction.EAST));

        // Central Decoration (The "Pond")
        mySpace.putBlock(new BlockPos(0, 0, 0), Blocks.COAL_BLOCK.defaultBlockState());
        mySpace.putBlock(new BlockPos(0, 1, 0), Blocks.FURNACE.defaultBlockState()
                .setValue(FurnaceBlock.LIT, true)
                .setValue(FurnaceBlock.FACING, Direction.EAST));

        // Glass Display (Shows Pane connections)
        BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
        mySpace.putBlock(new BlockPos(1, 0, -1), pane);
        mySpace.putBlock(new BlockPos(0, 0, -1), pane); // This connects to the wall AND the other pane!

        // 5. Lighting (Just for looks)
        mySpace.putBlock(new BlockPos(-1, 1, -1), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, false));
    }

    private static final ChestBlockEntity DUMMY_CHEST = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());

    private final PrismCamera renderCamera = PrismCamera.ISO_VIEW.copy();

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Get current time in ms
        long now = System.currentTimeMillis();

        this.targetProgress = (now % 6000 < 3000) ? (float) (now % 3000) / 3000 : 1f - (float) (now % 3000) / 3000;
        this.smoothProgress = PrismAnimation.easeInOutCubic(this.targetProgress);

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // --- HEADER ---
        PrismRenderer.renderGradientStringCenteredX(guiGraphics, font, Component.literal("Prism API - Test Screen").withStyle(ChatFormatting.BOLD),
                width / 2, 20, 2.5f, Color.RED, Color.BLUE, true);

        // Turn on the 3D Engine at screen coordinates, Scale 2.0x, Isometric View
        PrismRenderer.renderGradientStringCenteredXY(guiGraphics, font, Component.literal("Herobrine Altar"), width / 2, height / 2 + 60, 0.8f, Color.RED, Color.ORANGE, true);
        renderCamera.setRotX(30f).setRotY(225f + 360f * smoothProgress).setRotZ(0f).setZoom(1.5f).enable(guiGraphics, width / 2, height / 2);

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
        renderCamera.disable(guiGraphics);

        PrismRenderer.renderGradientStringCenteredXY(guiGraphics, font, Component.literal("Piston test"), width / 4, height / 2 + 60, 0.8f, Color.BLUE, Color.MAGENTA, true);
        renderCamera.setRotX(30f).setRotY(225f).setRotZ(0f).setZoom(1.5f).enable(guiGraphics, width / 4, height / 2);

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

        renderCamera.disable(guiGraphics);

        // --- SECONDARY SHOWCASE: COMPLEX MODELS ---
        // Positioned at 3/4 width, centered height
        renderCamera.setRotX(30f)
                .setRotY(225f - 360f * smoothProgress)
                .setRotZ(0f)
                .setZoom(1.5f + 0.5f * smoothProgress)
                .enable(guiGraphics, (width / 4) * 3, height / 2);

        PrismRenderer.renderSpace(guiGraphics, mySpace);

        renderCamera.disable(guiGraphics);

        // --- LABEL FOR COMPLEX ROW ---
        PrismRenderer.renderGradientStringCenteredXY(guiGraphics, font, Component.literal("Complex Geometry Showcase"),
                (width / 4) * 3, height / 2 + 60, 0.8f, Color.ORANGE, Color.YELLOW, true);

        Utilities.renderWatermark(guiGraphics, width, height);
    }
}