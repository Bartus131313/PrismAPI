package com.bartekkansy.prism.fabric;

import com.bartekkansy.prism.Prism;
import net.fabricmc.api.ModInitializer;

public final class PrismFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Prism.init();
    }
}
