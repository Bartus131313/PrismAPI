package com.bartekkansy.prism.api.fluid;

import com.bartekkansy.prism.api.util.PrismPlatform;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;

/**
 * A data-holding record that represents the state of a fluid container.
 * <p>
 * This record is used primarily by the rendering system to calculate
 * fill levels and generate localized tooltips.
 * </p>
 *
 * @param fluid    The {@link Fluid} type being stored.
 * @param amount   The current volume of fluid, typically in milliBuckets (mB).
 * @param capacity The maximum volume the container can hold.
 */
public record FluidTankInfo(Fluid fluid, int amount, int capacity) {
    /**
     * Calculates the fill ratio of the container.
     * <p>
     * The result is clamped between {@code 0.0f} and {@code 1.0f} to prevent
     * rendering artifacts in overfilled containers.
     * </p>
     *
     * @return A float representing the percentage filled (0.0 to 1.0).
     * Returns {@code 0.0f} if capacity is zero or negative.
     */
    public float getRatio() {
        if (capacity <= 0) return 0;
        return Math.min(1.0f, (float) amount / capacity);
    }

    /**
     * Retrieves the localized display name of the fluid.
     * <p>
     * This method delegates the naming logic to the platform-specific
     * {@link IPrismFluidHelper} to ensure compatibility with NeoForge
     * FluidTypes or Fabric FluidVariants.
     * </p>
     *
     * @return A {@link Component} containing the translated fluid name.
     */
    public Component getDisplayName() {
        return PrismPlatform.getFluidHelper().getDisplayName(this.fluid);
    }
}