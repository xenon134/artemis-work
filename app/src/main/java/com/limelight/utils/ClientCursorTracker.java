package com.limelight.utils;

/**
 * Keeps a best-effort estimate of the host cursor position in stream-view
 * coordinates.
 *
 * GameStream only gives us one-way input. Relative mouse moves tell us how far
 * the host cursor should have moved, but the host never reports where the
 * cursor actually is. Absolute mouse events are the exception: they explicitly
 * place the cursor, so we can use them as anchors.
 */
public class ClientCursorTracker {
    private float x;
    private float y;
    private float width;
    private float height;
    private boolean initialized;

    public synchronized void setBounds(float width, float height) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }

        if (!initialized) {
            this.width = width;
            this.height = height;
            this.x = width / 2.0f;
            this.y = height / 2.0f;
            initialized = true;
            return;
        }

        // Preserve the relative position across rotation, PiP, and display
        // changes instead of jumping back to the center.
        float relativeX = this.width > 0.0f ? x / this.width : 0.5f;
        float relativeY = this.height > 0.0f ? y / this.height : 0.5f;

        this.width = width;
        this.height = height;
        x = clamp(relativeX * width, 0.0f, width);
        y = clamp(relativeY * height, 0.0f, height);
    }

    public synchronized void resetToCenter() {
        x = width / 2.0f;
        y = height / 2.0f;
    }

    public synchronized void onRelativeMove(float deltaX, float deltaY) {
        if (!initialized) {
            return;
        }

        x = clamp(x + deltaX, 0.0f, width);
        y = clamp(y + deltaY, 0.0f, height);
    }

    public synchronized void onAbsolutePosition(float x, float y,
                                                float referenceWidth, float referenceHeight) {
        if (!initialized || referenceWidth <= 0.0f || referenceHeight <= 0.0f) {
            return;
        }

        this.x = clamp(x / referenceWidth * width, 0.0f, width);
        this.y = clamp(y / referenceHeight * height, 0.0f, height);
    }

    public synchronized void onRelativeMoveInReference(float deltaX, float deltaY,
                                                       float referenceWidth, float referenceHeight) {
        if (!initialized || referenceWidth <= 0.0f || referenceHeight <= 0.0f) {
            return;
        }

        onRelativeMove(deltaX * width / referenceWidth,
                deltaY * height / referenceHeight);
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    public synchronized float getX() {
        return x;
    }

    public synchronized float getY() {
        return y;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
