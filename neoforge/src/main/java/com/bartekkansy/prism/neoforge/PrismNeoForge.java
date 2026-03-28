package com.bartekkansy.prism.neoforge;

import com.bartekkansy.prism.Prism;
import net.neoforged.fml.common.Mod;

@Mod(Prism.MOD_ID)
public final class PrismNeoForge {
    public PrismNeoForge() {
        // Run our common setup.
        Prism.init();
    }
}
