package com.limelight.binding.input.touch;

import android.view.MotionEvent;

/**
 * Classifies a two-finger gesture as either a scroll or a pinch.
 *
 * The trackpad touch contexts need to know whether to turn a two-finger drag
 * into scroll packets or into a client-side viewport zoom. We delay the
 * decision until the gesture has moved far enough to be unambiguous, which
 * also gives us a chance to suppress any output until then.
 */
public class TrackpadGestureArbiter {
    public enum Mode {
        UNDECIDED,
        SCROLL,
        PINCH
    }

    // A span change smaller than this is not enough to call it a pinch.
    private static final float MIN_SPAN_DELTA_PX = 12.0f;
    // A pinch must change the span noticeably more than it moves the centroid.
    private static final float SPAN_DOMINANCE_FACTOR = 1.25f;
    // Minimum centroid movement before we commit to a scroll. The trackpad
    // contexts already wait 30px before scrolling, so waiting a little longer
    // here gives an ambiguous pinch more time to become obvious.
    private static final float SCROLL_SLOP_PX = 20.0f;

    private boolean tracking;
    private Mode mode = Mode.UNDECIDED;
    private float initialSpan;
    private float initialFocusX;
    private float initialFocusY;
    private float lastSpan;

    public void reset() {
        tracking = false;
        mode = Mode.UNDECIDED;
        initialSpan = 0;
        initialFocusX = 0;
        initialFocusY = 0;
        lastSpan = 0;
    }

    public void begin(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            reset();
            return;
        }

        tracking = true;
        mode = Mode.UNDECIDED;
        initialSpan = lastSpan = span(event);
        initialFocusX = focusX(event);
        initialFocusY = focusY(event);
    }

    public boolean isTracking() {
        return tracking;
    }

    public Mode classify(MotionEvent event) {
        if (!tracking || event.getPointerCount() < 2) {
            return Mode.UNDECIDED;
        }

        if (mode != Mode.UNDECIDED) {
            return mode;
        }

        float currentSpan = span(event);
        float spanDelta = Math.abs(currentSpan - initialSpan);
        float focusDeltaX = focusX(event) - initialFocusX;
        float focusDeltaY = focusY(event) - initialFocusY;
        float centroidDelta = (float) Math.sqrt(focusDeltaX * focusDeltaX + focusDeltaY * focusDeltaY);

        float pinchThreshold = Math.max(MIN_SPAN_DELTA_PX, initialSpan * 0.06f);

        if (spanDelta >= pinchThreshold && spanDelta >= centroidDelta * SPAN_DOMINANCE_FACTOR) {
            mode = Mode.PINCH;
        } else if (centroidDelta >= SCROLL_SLOP_PX) {
            mode = Mode.SCROLL;
        }

        return mode;
    }

    /**
     * Returns how much the span changed since the previous call, and updates
     * the previous span. The first call after a pinch is classified reports
     * the change since the gesture started.
     */
    public float getScaleDelta(MotionEvent event) {
        float currentSpan = span(event);
        float delta = lastSpan > 0 ? currentSpan / lastSpan : 1.0f;
        lastSpan = currentSpan;
        return delta;
    }

    public float getFocusX(MotionEvent event) {
        return focusX(event);
    }

    public float getFocusY(MotionEvent event) {
        return focusY(event);
    }

    private static float span(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float focusX(MotionEvent event) {
        return (event.getX(0) + event.getX(1)) / 2.0f;
    }

    private static float focusY(MotionEvent event) {
        return (event.getY(0) + event.getY(1)) / 2.0f;
    }
}
