package com.bartekkansy.prism.api.util.fabric;

import com.bartekkansy.prism.api.fluid.IPrismFluidHelper;
import com.bartekkansy.prism.fabric.PrismFluidHelperFabric;

public class PrismPlatformImpl {
    public static IPrismFluidHelper getFluidHelper() {
        return new PrismFluidHelperFabric();
    }
}