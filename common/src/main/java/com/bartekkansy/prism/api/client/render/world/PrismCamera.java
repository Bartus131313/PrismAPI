package com.bartekkansy.prism.api.client.render.world;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Handles the 3D-on-2D projection state for the Prism API.
 * This class manages rotations, scaling, and the lighting setup required to
 * render world objects inside a GUI context.
 */
public class PrismCamera {
    private float rotX;
    private float rotY;
    private float rotZ;
    private float zoom;

    public static final PrismCamera ISO_VIEW = new PrismCamera(30, 225, 0);

    /**
     * Creates a new camera with specific rotation and zoom values.
     * @param rotX Rotation around the X-axis (Pitch).
     * @param rotY Rotation around the Y-axis (Yaw).
     * @param rotZ Rotation around the Z-axis (Roll).
     * @param zoom The scale multiplier (1.0f is standard 16x16 GUI size).
     */
    public PrismCamera(float rotX, float rotY, float rotZ, float zoom) {
        this.rotX = rotX;
        this.rotY = rotY;
        this.rotZ = rotZ;
        this.zoom = zoom;
    }

    /**
     * Creates a camera with default 1.0x zoom.
     */
    public PrismCamera(float xRot, float yRot, float zRot) {
        this(xRot, yRot, zRot, 1.0f);
    }

    /**
     * Creates a camera with no rotation and 1.0x zoom.
     */
    public PrismCamera() {
        this(0f, 0f, 0f, 1.0f);
    }

    /**
     * Prepares the rendering state for 3D objects.
     * Pushes a new pose, applies transformations, and enables the depth buffer.
     * @param guiGraphics The current GUI graphics context.
     * @param x The horizontal center for the 3D render.
     * @param y The vertical center for the 3D render.
     */
    public void enable(GuiGraphics guiGraphics, int x, int y) {
        // Get PoseStack from GuiGraphics
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // Move camera to the X/Y position on the screen
        // TODO: Automatically get Z-Level
        poseStack.translate(x, y, 200f);

        // Scale up to block size and flip the Y axis (GUI is Y-down, World is Y-up)
        float finalScale = 16.0F * zoom;
        poseStack.scale(finalScale, -finalScale, finalScale);

        // Apply Camera Rotation
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));

        // Enable Depth Buffer
        RenderSystem.enableDepthTest();

        // Setup lighting so blocks aren't pitch black
        Lighting.setupFor3DItems();
    }

    /**
     * Cleans up the 3D rendering state.
     * Flushes the buffer source to ensure blocks are rendered before the pose is popped.
     * @param guiGraphics The current GUI graphics context.
     */
    public void disable(GuiGraphics guiGraphics) {
        // We must end the batch before popping the pose!
        // If we pop the pose before flushing, the blocks will draw in the wrong place.
        guiGraphics.bufferSource().endBatch();

        // Turn off 3D depth so normal tooltips and text don't break
        RenderSystem.disableDepthTest();

        // Pop the camera matrix
        guiGraphics.pose().popPose();
    }

    /**
     * Creates a deep copy of this camera instance.
     * <p>
     * Useful for saving a "snapshot" of a camera state before performing
     * animations or temporary transformations.
     * </p>
     * @return A new {@link PrismCamera} instance with the same rotation and zoom values.
     */
    public PrismCamera copy() {
        return new PrismCamera(this.rotX, this.rotY, this.rotZ, this.zoom);
    }

    // --- Fluent Setters (Return 'this' for chaining) ---

    /** Sets the X rotation and returns the camera instance for chaining. */
    public PrismCamera setRotX(float rotX) {
        this.rotX = rotX;
        return this;
    }

    /** Sets the Y rotation and returns the camera instance for chaining. */
    public PrismCamera setRotY(float rotY) {
        this.rotY = rotY;
        return this;
    }

    /** Sets the Z rotation and returns the camera instance for chaining. */
    public PrismCamera setRotZ(float rotZ) {
        this.rotZ = rotZ;
        return this;
    }

    /** Sets the zoom/scale and returns the camera instance for chaining. */
    public PrismCamera setZoom(float zoom) {
        this.zoom = zoom;
        return this;
    }

    // --- Getters ---

    public float getRotX() {
        return rotX;
    }
    public float getRotY() {
        return rotY;
    }
    public float getRotZ() {
        return rotZ;
    }
    public float getZoom() {
        return zoom;
    }
}
