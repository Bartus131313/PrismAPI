package com.bartekkansy.prism.fabric;

import com.bartekkansy.prism.api.client.render.IPrismFluidHelper;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;

public class PrismFluidHelperFabric implements IPrismFluidHelper {
    @Override
    public TextureAtlasSprite getStillSprite(Fluid fluid) {
        // Fabric uses FluidVariant for rendering info
        return FluidVariantRendering.getSprite(FluidVariant.of(fluid));
    }

    @Override
    public int getColor(Fluid fluid) {
        return FluidVariantRendering.getColor(FluidVariant.of(fluid));
    }
}