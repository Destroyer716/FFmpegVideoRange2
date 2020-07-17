package com.example.myplayer;

import java.util.Arrays;

/**
 * Created By Ele
 * on 2020/6/14
 **/
public class PacketBean {

    private double pts;
    private int dataSize;
    private byte[] data;
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

    public double getPts() {
        return pts;
    }

    public void setPts(double pts) {
        this.pts = pts;
    }

    public int getDataSize() {
        return dataSize;
    }

    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }


    @Override
    public String toString() {
        return "PacketBean{" +
                "pts=" + pts +
                ", dataSize=" + dataSize +
                ", data=" + Arrays.toString(data) +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
