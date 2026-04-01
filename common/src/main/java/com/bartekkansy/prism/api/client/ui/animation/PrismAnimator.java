package com.bartekkansy.prism.api.client.ui.animation;

/**
 * A comprehensive utility for smooth UI animations and transitions.
 * Includes standard linear interpolation and a variety of easing functions
 * to create organic, professional-feeling motion.
 */
public class PrismAnimator {

    /**
     * Linearly interpolates between two values based on a delta.
     */
    public static float lerp(float start, float end, float delta) {
        return start + delta * (end - start);
    }

    /**
     * A classic S-curve that starts and ends slowly with a faster middle.
     */
    public static float smoothStep(float x) {
        return x * x * (3.0f - 2.0f * x);
    }

    /**
     * Accelerates using a sine curve.
     */
    public static float easeInSine(float x) {
        return 1.0f - (float) Math.cos((x * Math.PI) / 2.0f);
    }

    /**
     * Decelerates using a sine curve.
     */
    public static float easeOutSine(float x) {
        return (float) Math.sin((x * Math.PI) / 2.0f);
    }

    /**
     * Combined sine curve: slow start, fast middle, slow end.
     */
    public static float easeInOutSine(float x) {
        return -(float)(Math.cos(Math.PI * x) - 1) / 2.0f;
    }

    /**
     * Accelerates with a cubic curve.
     */
    public static float easeInCubic(float x) {
        return x * x * x;
    }

    /**
     * Decelerates with a cubic curve.
     */
    public static float easeOutCubic(float x) {
        return 1.0f - (float)Math.pow(1.0f - x, 3.0f);
    }

    /**
     * Combined cubic curve: aggressive acceleration/deceleration.
     */
    public static float easeInOutCubic(float x) {
        return x < 0.5f ? 4.0f * x * x * x : 1.0f - (float) Math.pow(-2.0f * x + 2.0f, 3.0f) / 2.0f;
    }

    /**
     * Very sharp acceleration.
     */
    public static float easeInQuint(float x) {
        return x * x * x * x * x;
    }

    /**
     * Very sharp deceleration.
     */
    public static float easeOutQuint(float x) {
        return 1.0f - (float) Math.pow(1.0f - x, 5.0f);
    }

    /**
     * Extremely sharp combined curve for dramatic transitions.
     */
    public static float easeInOutQuint(float x) {
        return x < 0.5f ? 16.0f * x * x * x * x * x : 1.0f - (float) Math.pow(-2.0f * x + 2.0f, 5.0f) / 2.0f;
    }

    /**
     * Accelerates using a circular function.
     */
    public static float easeInCirc(float x) {
        return 1.0f - (float) Math.sqrt(1.0f - Math.pow(x, 2.0f));
    }

    /**
     * Decelerates using a circular function.
     */
    public static float easeOutCirc(float x) {
        return (float) Math.sqrt(1.0f - Math.pow(x - 1.0f, 2.0f));
    }

    /**
     * Circular acceleration and deceleration.
     */
    public static float easeInOutCirc(float x) {
        return x < 0.5f
                ? (1.0f - (float) Math.sqrt(1.0f - (float) Math.pow(2.0f * x, 2.0f))) / 2.0f
                : (float) (Math.sqrt(1 - (float) Math.pow(-2.0f * x + 2.0f, 2.0f)) + 1.0f) / 2.0f;
    }

    /**
     * Oscillates back and forth before accelerating forward.
     */
    public static float easeInElastic(float x) {
        float c4 = (2.0f * (float) Math.PI) / 3.0f;

        return x == 0.0f ? 0.0f : x == 1.0f ? 1.0f : -(float) Math.pow(2.0f, 10.0f * x - 10.0f) * (float) Math.sin((x * 10.0f - 10.75f) * c4);
    }

    /**
     * Decelerates while bouncing like a rubber band.
     */
    public static float easeOutElastic(float x) {
        float c4 = (2.0f * (float) Math.PI) / 3.0f;

        return x == 0.0f ? 0.0f : x == 1.0f ? 1.0f : (float) Math.pow(2.0f, -10.0f * x) * (float) Math.sin((x * 10.0f - 0.75f) * c4) + 1.0f;
    }

    /**
     * Combined elastic motion.
     */
    public static float easeInOutElastic(float x) {
        float c5 = (2.0f * (float) Math.PI) / 4.5f;

        return x == 0.0f ? 0.0f : x == 1.0f ? 1.0f
                : x < 0.5f ? -((float) Math.pow(2.0f, 20.0f * x - 10.0f) * (float) Math.sin((20.0f * x - 11.125f) * c5)) / 2.0f
                : ((float) Math.pow(2.0f, -20.0f * x + 10.0f) * (float) Math.sin((20.0f * x - 11.125) * c5)) / 2.0f + 1.0f;
    }

    /**
     * Accelerates using a square (quad) function.
     */
    public static float easeInQuad(float x) {
        return x * x;
    }

    /**
     * Decelerates using a square (quad) function.
     */
    public static float easeOutQuad(float x) {
        return 1.0f - (1.0f - x) * (1.0f - x);
    }

    /**
     * Combined quad curve.
     */
    public static float easeInOutQuad(float x) {
        return x < 0.5f ? 2.0f * x * x : 1.0f - (float) Math.pow(-2.0f * x + 2.0f, 2.0f) / 2.0f;
    }

    /**
     * Accelerates using a quart function.
     */
    public static float easeInQuart(float x) {
        return x * x * x * x;
    }

    /**
     * Decelerates using a quart function.
     */
    public static float easeOutQuart(float x) {
        return 1.0f - (float) Math.pow(1.0f - x, 4.0f);
    }

    /**
     * Combined quart curve.
     */
    public static float easeInOutQuart(float x) {
        return x < 0.5f ? 8.0f * x * x * x * x : 1.0f - (float) Math.pow(-2.0f * x + 2.0f, 4.0f) / 2.0f;
    }

    /**
     * Accelerates exponentially.
     */
    public static float easeInExpo(float x) {
        return x == 0.0f ? 0.0f : (float) Math.pow(2.0f, 10.0f * x - 10.0f);
    }

    /**
     * Decelerates exponentially.
     */
    public static float easeOutExpo(float x) {
        return x == 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0f, -10.0f * x);
    }

    /**
     * Combined exponential curve.
     */
    public static float easeInOutExpo(float x) {
        return x == 0.0f ? 0.0f : x == 1.0f ? 1.0f
                : x < 0.5f ? (float) Math.pow(2.0f, 20.0f * x - 10.0f) / 2.0f
                : (2.0f - (float) Math.pow(2.0f, -20.0f * x + 10.0f)) / 2.0f;
    }

    /**
     * Retracts slightly before moving forward.
     */
    public static float easeInBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;

        return c3 * x * x * x - c1 * x * x;
    }

    /**
     * Over shoots the target before settling back.
     */
    public static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;

        return 1.0f + c3 * (float) Math.pow(x - 1.0f, 3.0f) + c1 * (float) Math.pow(x - 1.0f, 2.0f);
    }

    /**
     * Combined back-and-forth motion.
     */
    public static float easeInOutBack(float x) {
        float c1 = 1.70158f;
        float c2 = c1 * 1.525f;

        return x < 0.5f
                ? ((float) Math.pow(2.0f * x, 2.0f) * ((c2 + 1.0f) * 2.0f * x - c2)) / 2.0f
                : ((float) Math.pow(2.0f * x - 2.0f, 2.0f) * ((c2 + 1.0f) * (x * 2.0f - 2.0f) + c2) + 2.0f) / 2.0f;
    }

    /**
     * A "bouncing ball" deceleration effect.
     */
    public static float easeOutBounce(float x) {
        float n1 = 7.5625f;
        float d1 = 2.75f;

        if (x < 1 / d1) {
            return n1 * x * x;
        } else if (x < 2f / d1) {
            return n1 * (x -= 1.5f / d1) * x + 0.75f;
        } else if (x < 2.5f / d1) {
            return n1 * (x -= 2.25f / d1) * x + 0.9375f;
        } else {
            return n1 * (x -= 2.625f / d1) * x + 0.984375f;
        }
    }

    /**
     * A "bouncing ball" acceleration effect.
     */
    public static float easeInBounce(float x) {
        return 1.0f - easeOutBounce(1.0f - x);
    }

    /**
     * Combined bounce effect for complex UI impact.
     */
    public static float easeInOutBounce(float x) {
        return x < 0.5f
                ? (1.0f - easeOutBounce(1.0f - 2.0f * x)) / 2.0f
                : (1.0f + easeOutBounce(2.0f * x - 1.0f)) / 2.0f;
    }
}