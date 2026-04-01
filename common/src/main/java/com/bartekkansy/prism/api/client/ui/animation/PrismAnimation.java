package com.bartekkansy.prism.api.client.ui.animation;

import net.minecraft.client.gui.GuiGraphics;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Handles time-based animations for UI elements using easing functions.
 * Supports finite repetitions and infinite looping.
 */
public class PrismAnimation {
    private final long startTime;
    private final float durationMillis;
    private final Function<Float, Float> easing;
    private final int repeatCount;

    /**
     * Creates a new animation instance.
     *
     * @param duration    The duration of a single animation cycle in seconds.
     * @param easing      The easing function to apply to the progress.
     * @param repeatCount Total times to repeat. Use {@code -1} for infinite looping.
     */
    public PrismAnimation(float duration, Function<Float, Float> easing, int repeatCount) {
        this.startTime = System.currentTimeMillis();
        this.durationMillis = duration * 1000f;
        this.easing = easing;
        this.repeatCount = repeatCount;
    }

    /**
     * Creates a new animation instance with infinite looping.
     *
     * @param duration    The duration of a single animation cycle in seconds.
     * @param easing      The easing function to apply to the progress.
     */
    public PrismAnimation(float duration, Function<Float, Float> easing) {
        this(duration, easing, -1);
    }

    /**
     * Calculates the current animation progress and executes the renderer.
     * <p>
     * If the animation is looping, the progress resets to 0.0 after each cycle.
     *
     * @param guiGraphics The current {@link GuiGraphics} context.
     * @param renderer    A consumer that receives {@link AnimationInfo} with eased progress.
     */
    public void render(GuiGraphics guiGraphics, Consumer<AnimationInfo> renderer) {
        long elapsed = System.currentTimeMillis() - startTime;
        float linearProgress;

        if (repeatCount == -1) {
            // Infinite loop: map elapsed time to 0.0-1.0 cycle
            linearProgress = (elapsed % (long) durationMillis) / durationMillis;
        } else {
            // Finite repetitions: check if we exceeded total duration
            long totalDuration = (long) (durationMillis * (repeatCount + 1));

            if (elapsed >= totalDuration) linearProgress = 1.0f;
            else linearProgress = (elapsed % (long) durationMillis) / durationMillis;
        }

        float animatedProgress = easing.apply(linearProgress);
        renderer.accept(new AnimationInfo(guiGraphics, animatedProgress));
    }

    /**
     * Checks if the animation has finished all assigned repetitions.
     * Always returns {@code false} if {@code repeatCount} is {@code -1}.
     *
     * @return {@code true} if all cycles are complete.
     */
    public boolean isFinished() {
        if (repeatCount == -1) return false;
        long totalDuration = (long) (durationMillis * (repeatCount + 1));
        return System.currentTimeMillis() - startTime >= totalDuration;
    }
}