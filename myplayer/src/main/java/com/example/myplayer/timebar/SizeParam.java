package com.example.myplayer.timebar;

public class SizeParam {
    //一个大刻度代表的度量
    private final int largeValue;
    // 一个小刻度代表的度量
    private final int unitValue;
    //当前最小刻度与最小单位的进制 如:当前最小刻度表示1分钟,则1分钟有 1*60*1000毫秒,此时 decimal 为60*1000
    private final int decimal;
    /*显示区域最多显示大刻度的个数*/
    private final int disPlayLargeCount;

    public SizeParam(int largeValue, int unitValue, int decimal, int disPlayLargeCount) {
        this.largeValue = largeValue;
        this.unitValue = unitValue;
        this.decimal = decimal;
        this.disPlayLargeCount = disPlayLargeCount;
    }

    public int getDecimal() {
        return decimal;
    }

    public int getUnitValue() {
        return unitValue;
    }

    public int getLargeValue() {
        return largeValue;
    }

    public int getDisPlayLargeCount() {
        return disPlayLargeCount;
    }
}
