package com.bartekkansy.prism.api.util.neoforge;

import com.bartekkansy.prism.api.fluid.IPrismFluidHelper;
import com.bartekkansy.prism.neoforge.PrismFluidHelperNeo;

public class PrismPlatformImpl {
    public static IPrismFluidHelper getFluidHelper() {
        return new PrismFluidHelperNeo();
    }
}
