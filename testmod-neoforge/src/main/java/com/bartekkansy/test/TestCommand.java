package com.bartekkansy.test;

import com.bartekkansy.test.screen.TestScreen;
import com.bartekkansy.test.screen.DemoScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TestCommand {

    /**
     * Registers the /prism command with sub-commands for demo and test screens.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("prism")
                // Base command /prism (defaults to demo or shows help)
                .executes(context -> {
                    context.getSource().sendSystemMessage(Component.literal("§b[Prism API]§r Use /prism demo or /prism test"));
                    return 1;
                })
                // Sub-command: /prism demo
                .then(Commands.literal("demo")
                        .executes(context -> {
                            openScreen(new DemoScreen()); // Assuming you have a DemoScreen
                            return 1;
                        })
                )
                // Sub-command: /prism test
                .then(Commands.literal("test")
                        .executes(context -> {
                            openScreen(new TestScreen());
                            return 1;
                        })
                )
        );
    }

    /**
     * Helper to ensure screens open safely on the main thread.
     */
    private static void openScreen(net.minecraft.client.gui.screens.Screen screen) {
        Minecraft.getInstance().tell(() -> {
            Minecraft.getInstance().setScreen(screen);
        });
    }
}