package com.bartekkansy.test.util;

import com.bartekkansy.prism.api.client.render.PrismRenderer;
import com.bartekkansy.prism.api.util.PrismConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;

public class Utilities {

    public static void renderWatermark(GuiGraphics guiGraphics, int width, int height) {
        Font font = Minecraft.getInstance().font;

        PrismRenderer.renderString(guiGraphics, font, Component.literal("Prism API v" + PrismConfig.getVersion()).withStyle(ChatFormatting.ITALIC),
                10, height - 15, 0.8f, Color.LIGHT_GRAY, false);

        PrismRenderer.renderString(guiGraphics, font, Component.literal("DEVELOPER BUILD"),
                10, height - 25, 0.5f, Color.GRAY, false);
    }
}
