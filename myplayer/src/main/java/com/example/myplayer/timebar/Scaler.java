package com.example.myplayer.timebar;


public class Scaler {
    private int position;
    /*是否为关键刻度*/
    private boolean isKeyScaler;

    public void setPosition(int value) {
        this.position = value;
    }

    public int getPosition() {
        return position;
    }

    public boolean isKeyScaler() {
        return isKeyScaler;
    }

    public void setKeyScaler(boolean keyScaler) {
        isKeyScaler = keyScaler;
    }

}
