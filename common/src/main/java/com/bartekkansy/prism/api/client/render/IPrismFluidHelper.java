package com.bartekkansy.prism.api.client.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;

public interface IPrismFluidHelper {
    TextureAtlasSprite getStillSprite(Fluid fluid);
    int getColor(Fluid fluid);
}
