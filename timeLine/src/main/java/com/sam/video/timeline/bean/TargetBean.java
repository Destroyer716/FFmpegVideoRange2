package com.sam.video.timeline.bean;

public class TargetBean {
    private long timeUs;
    private boolean isAddFrame = false;
    private boolean removeTag = false;

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

    public boolean isRemoveTag() {
        return removeTag;
    }

    public void setRemoveTag(boolean removeTag) {
        this.removeTag = removeTag;
    }

    @Override
    public String toString() {
        return "TargetBean{" +
                "timeUs=" + timeUs +
                ", isAddFrame=" + isAddFrame +
                ", removeTag=" + removeTag +
                '}';
    }

}
