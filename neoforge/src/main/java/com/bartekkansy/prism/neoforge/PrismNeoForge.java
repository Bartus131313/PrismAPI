package com.bartekkansy.prism.neoforge;

import com.bartekkansy.prism.Prism;
import com.bartekkansy.prism.neoforge.client.PrismTestScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(Prism.MOD_ID)
public final class PrismNeoForge {
    public PrismNeoForge() {
        // Run our common setup.
        Prism.init();

        // Listen to the NeoForge Global Event Bus
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        // If you press 'P' (for Prism), open the test screen
        if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_P)) {
            if (!(Minecraft.getInstance().screen instanceof PrismTestScreen)) {
                Minecraft.getInstance().setScreen(new PrismTestScreen());
            }
        }
    }
}
