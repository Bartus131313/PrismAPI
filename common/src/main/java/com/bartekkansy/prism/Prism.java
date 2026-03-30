package com.bartekkansy.prism;

import net.minecraft.ChatFormatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Prism {
    public static final String MOD_ID = "prism";
    public static final Logger LOGGER = LoggerFactory.getLogger("Prism API");

    public static void init() {
        // Get the version automatically from the Mod Container
        String version = getVersion();

        // Determine if it's a Dev build (checks for "SNAPSHOT", "dev", or "beta")
        boolean isDev = version.contains("SNAPSHOT") || version.contains("dev") || version.contains("beta");

        if (isDev) printDevWarning(version);

        LOGGER.info("[Prism API] v{} initialized successfully.", version);
    }

    private static void printDevWarning(String version) {
        String yellow = ChatFormatting.YELLOW.toString();
        String reset = ChatFormatting.RESET.toString();

        LOGGER.warn("{}-----------------------------------------{}", yellow, reset);
        LOGGER.warn("{}[Prism API] RUNNING IN DEVELOPMENT MODE{}", yellow, reset);
        LOGGER.warn("{}> Version: v{}{}", yellow, version, reset);
        LOGGER.warn("{}> This build is unstable and intended for testing.{}", yellow, reset);
        LOGGER.warn("{}> Stable builds: https://github.com/Bartus131313/PrismAPI{}", yellow, reset);
        LOGGER.warn("{}-----------------------------------------{}", yellow, reset);
    }

    private static String getVersion() {
        return "1.1.0-SNAPSHOT";
    }
}