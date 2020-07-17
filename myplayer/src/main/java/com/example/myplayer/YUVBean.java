package com.example.myplayer;

/**
 * Created By Ele
 * on 2020/6/9
 **/
public class YUVBean {
    private byte[] yData;
    private byte[] uData;
    private byte[] vData;
    private double timestamp;
    private int width;
    private int height;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public byte[] getyData() {
        return yData;
    }

    public void setyData(byte[] yData) {
        this.yData = yData;
    }

    public byte[] getuData() {
        return uData;
    }

    public void setuData(byte[] uData) {
        this.uData = uData;
    }

    public byte[] getvData() {
        return vData;
    }

    public void setvData(byte[] vData) {
        this.vData = vData;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }
}
