package com.limelight.binding.input.touch;

public interface TouchContext {
    int getActionIndex();
    void setPointerCount(int pointerCount);
    boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger);
    boolean touchMoveEvent(int eventX, int eventY, long eventTime);
    void touchUpEvent(int eventX, int eventY, long eventTime);
    void cancelTouch();
    boolean isCancelled();

    /**
     * When suppressed, the context may keep tracking the gesture but must not
     * emit any input to the server. Used while a two-finger gesture is still
     * being classified as scroll vs pinch.
     */
    default void setOutputSuppressed(boolean suppressed) {}
}
