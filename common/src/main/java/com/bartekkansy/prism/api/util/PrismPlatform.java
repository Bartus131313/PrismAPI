package com.bartekkansy.prism.api.util;

import com.bartekkansy.prism.api.fluid.IPrismFluidHelper;
import dev.architectury.injectables.annotations.ExpectPlatform;

/**
 * Universal entry point for platform-specific API implementations.
 * <p>
 * This class utilizes Architectury's {@link ExpectPlatform} system to provide
 * a unified interface for both NeoForge and Fabric. The actual implementation
 * is swapped at compile-time or runtime depending on the environment.
 * </p>
 */
public class PrismPlatform {

    /**
     * Retrieves the platform-specific fluid helper.
     * <p>
     * On NeoForge, this returns an implementation utilizing {@code FluidType}.
     * On Fabric, this returns an implementation utilizing the {@code Fabric Fluid API}.
     * </p>
     *
     * @return The active {@link IPrismFluidHelper} for the current loader.
     * @throws AssertionError If the platform-specific implementation is missing.
     */
    @ExpectPlatform
    public static IPrismFluidHelper getFluidHelper() {
        throw new AssertionError();
    }
}