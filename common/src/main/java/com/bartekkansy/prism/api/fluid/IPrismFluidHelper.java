package com.bartekkansy.prism.api.fluid;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;

/**
 * A platform-agnostic bridge for retrieving client-side {@link Fluid} information.
 * <p>
 * This interface must be implemented separately for each mod loader (e.g., NeoForge and Fabric)
 * to handle their respective fluid registration and attribute systems.
 * </p>
 *
 * @see com.bartekkansy.prism.api.util.PrismPlatform#getFluidHelper()
 */
public interface IPrismFluidHelper {
    /**
     * Retrieves the "Still" texture sprite for the given fluid.
     * @param fluid     The {@link Fluid} to query.
     * @return          The {@link TextureAtlasSprite} used for the fluid when it is not flowing.
     * @apiNote         Implementation should typically use {@code FluidVariantAttributes} on Fabric
     * or {@code IClientFluidTypeExtensions} on NeoForge.
     */
    TextureAtlasSprite getStillSprite(Fluid fluid);

    /**
     * Retrieves the ARGB color tint of the given fluid.
     * <p>
     * This color is used to apply proper shading to fluid textures (e.g., the blue tint of water).
     * </p>
     * @param fluid The {@link Fluid} to query.
     * @return An integer representing the ARGB color.
     * @apiNote Example: {@code 0xFF3F76E4} for standard Minecraft Water.
     */
    int getColor(Fluid fluid);

    /**
     * Retrieves the localized display name of the fluid.
     * @param fluid The {@link Fluid} to query.
     * @return A {@link Component} containing the translated name.
     */
    Component getDisplayName(Fluid fluid);
}
