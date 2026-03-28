package com.bartekkansy.prism.neoforge;

import com.bartekkansy.prism.api.client.render.IPrismFluidHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public class PrismFluidHelperNeo implements IPrismFluidHelper {
    @Override
    public TextureAtlasSprite getStillSprite(Fluid fluid) {
        ResourceLocation tex = IClientFluidTypeExtensions.of(fluid).getStillTexture();
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(tex);
    }

    @Override
    public int getColor(Fluid fluid) {
        return IClientFluidTypeExtensions.of(fluid).getTintColor();
    }
}