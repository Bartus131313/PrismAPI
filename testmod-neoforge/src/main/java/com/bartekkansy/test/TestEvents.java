package com.bartekkansy.test;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "prism", bus = EventBusSubscriber.Bus.GAME)
public class TestEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TestCommand.register(event.getDispatcher());
    }
}