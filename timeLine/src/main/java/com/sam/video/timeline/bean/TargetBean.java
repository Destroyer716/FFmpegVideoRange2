package com.sam.video.timeline.bean;

public class TargetBean {
    private long timeUs;
    private boolean isAddFrame = false;

    public long getTimeUs() {
        return timeUs;
    }

    public void setTimeUs(long timeUs) {
        this.timeUs = timeUs;
    }

    public boolean isAddFrame() {
        return isAddFrame;
    }

    public void setAddFrame(boolean addFrame) {
        isAddFrame = addFrame;
    }

    @Override
    public String toString() {
        return "TargetBean{" +
                "timeUs=" + timeUs +
                ", isAddFrame=" + isAddFrame +
                '}';
    }

}
