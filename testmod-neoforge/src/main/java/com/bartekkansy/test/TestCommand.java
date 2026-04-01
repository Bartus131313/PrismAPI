package com.bartekkansy.test;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class TestCommand {

    /**
     * Registers a simple debug command to open the Prism Showcase.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("prism")
                .executes(context -> {
                    Minecraft.getInstance().tell(() -> {
                        Minecraft.getInstance().setScreen(new TestScreen());
                    });
                    return 1;
                })
        );
    }
}