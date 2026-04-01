package com.bartekkansy.test;

import com.bartekkansy.prism.api.client.render.PrismRenderer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.apache.logging.log4j.core.net.Priority;
import org.slf4j.Logger;

import java.awt.*;

@Mod(TestMod.MOD_ID)
public class TestMod {

    public static final String MOD_ID = "testmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TestMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        NeoForge.EVENT_BUS.register(this);
    }

//    @SubscribeEvent
//    public void onOverlayRender(RenderGuiLayerEvent.Pre event) {
//        GuiGraphics guiGraphics = event.getGuiGraphics();
//
//        Font font = Minecraft.getInstance().font;
//        Component component = Component.literal(String.format("FPS: %s", Minecraft.getInstance().getFps()));
//
//        PrismRenderer.renderGradientString(guiGraphics, font, component, 12, 12, 1f, Color.BLUE, Color.MAGENTA, true);
//    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        TestCommand.register(event.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}
