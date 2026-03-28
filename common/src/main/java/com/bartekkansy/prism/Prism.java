package com.bartekkansy.prism;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Prism {
    public static final String MOD_ID = "prism";
    public static final Logger LOGGER = LoggerFactory.getLogger("Prism API");

    public static void init() {
        LOGGER.info("Prism API is initializing...");
    }
}
