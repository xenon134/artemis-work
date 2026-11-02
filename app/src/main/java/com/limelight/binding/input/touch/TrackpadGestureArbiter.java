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

    // RVNC classifies two-finger gestures by comparing the change in finger
    // separation against the total distance both fingers have travelled:
    //
    //     ratio = abs(currentSpan - initialSpan)
    //             / (finger0Travel + finger1Travel)
    //
    // Fingers moving together (scroll) produce a large denominator and a small
    // numerator. Fingers spreading or pinching produce almost the same value in
    // both, so the ratio approaches 1.0. This is much less sensitive to the
    // gesture's direction than comparing span against centroid movement.
    private static final float PINCH_RATIO = 0.7f;
    private static final float SCROLL_RATIO = 0.2f;
    // Each finger must move at least this far before the ratio is meaningful.
    // This defaults to 12px but Game replaces it with 12dp to match RVNC.
    private float fingerMovementThresholdPx = 12.0f;
    // Some slow gestures produce many tiny move events before crossing the
    // distance threshold. After enough events, treat both fingers as moved so
    // the gesture can still be classified.
    private static final int MOVEMENT_FALLBACK_EVENTS = 10;

    private boolean tracking;
    private Mode mode = Mode.UNDECIDED;
    private final float[] initialX = new float[2];
    private final float[] initialY = new float[2];
    private final boolean[] fingerMoved = new boolean[2];
    private int moveEvents;
    private float initialSpan;
    private float lastSpan;

    public void setMovementThresholdPx(float thresholdPx) {
        fingerMovementThresholdPx = Math.max(1.0f, thresholdPx);
    }

    public void reset() {
        tracking = false;
        mode = Mode.UNDECIDED;
        initialSpan = 0;
        lastSpan = 0;
        moveEvents = 0;
        fingerMoved[0] = false;
        fingerMoved[1] = false;
    }

    public void begin(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            reset();
            return;
        }

        tracking = true;
        mode = Mode.UNDECIDED;
        initialSpan = lastSpan = span(event);
        moveEvents = 0;
        for (int i = 0; i < 2; i++) {
            initialX[i] = event.getX(i);
            initialY[i] = event.getY(i);
            fingerMoved[i] = false;
        }
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

        moveEvents++;
        if (moveEvents > MOVEMENT_FALLBACK_EVENTS) {
            fingerMoved[0] = true;
            fingerMoved[1] = true;
        }
        for (int i = 0; i < 2; i++) {
            if (!fingerMoved[i]) {
                fingerMoved[i] = distance(initialX[i], initialY[i],
                        event.getX(i), event.getY(i)) >= fingerMovementThresholdPx;
            }
        }
        if (!fingerMoved[0] || !fingerMoved[1]) {
            return Mode.UNDECIDED;
        }

        float currentSpan = span(event);
        float spanChange = Math.abs(currentSpan - initialSpan);
        float totalMovement = distance(initialX[0], initialY[0], event.getX(0), event.getY(0))
                + distance(initialX[1], initialY[1], event.getX(1), event.getY(1));
        if (totalMovement <= 0.0f) {
            return Mode.UNDECIDED;
        }

        float ratio = spanChange / totalMovement;
        if (ratio > PINCH_RATIO) {
            mode = Mode.PINCH;
        } else if (ratio < SCROLL_RATIO) {
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

    private static float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float span(MotionEvent event) {
        return distance(event.getX(0), event.getY(0),
                event.getX(1), event.getY(1));
    }

    private static float focusX(MotionEvent event) {
        return (event.getX(0) + event.getX(1)) / 2.0f;
    }

    private static float focusY(MotionEvent event) {
        return (event.getY(0) + event.getY(1)) / 2.0f;
    }
}
