package com.bartekkansy.prism.api.util;

import com.bartekkansy.prism.api.client.render.IPrismFluidHelper;
import dev.architectury.injectables.annotations.ExpectPlatform;

public class PrismPlatform {
    @ExpectPlatform
    public static IPrismFluidHelper getFluidHelper() {
        // Architectury will automatically swap this for the
        // NeoForge or Fabric version at runtime.
        throw new AssertionError();
    }
}