package com.example.myplayer.timebar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ScaleModel {

    public enum UnitModel {
        UNITVALUE_1_MIN,                /*模式一：最小刻度1分钟,一个大刻度５分钟*/
        UNITVALUE_5_MIN,                /*模式二：最小刻度5分钟,一个大刻度10分钟*/
        UNITVALUE_10_MIN,               /*模式三：最小刻度10分钟,一个大刻度30分钟*/
        UNITVALUE_30_MIN,               /*模式四：最小刻度30分钟,一个大刻度1小时*/
        UNITVALUE_1_HOUR;               /*模式五：最小刻度1小时,一个大刻度２小时*/
    }

    private final HashMap<UnitModel, SizeParam> sizeParamMap = new HashMap<>();
    private final List<Scaler> scaleList = new ArrayList<>();
    private long sartValue;
    private long endValue;

    /*显示区域/可见区域*/
    private int disPlayWidth;
    private SizeParam currSizeParam;

/*    public ScaleModel() {
        SizeParam model = null;
        //模式一：最小刻度1分钟,一个大刻度５分钟
        model = new SizeParam(5, 1, 60 * 1000, 16);
        sizeParamMap.put(UnitModel.UNITVALUE_1_MIN, model);
        //模式二：最小刻度5分钟,一个大刻度10分钟
        model = new SizeParam(10, 5, 60 * 1000, 14);
        sizeParamMap.put(UnitModel.UNITVALUE_5_MIN, model);
        //模式三：最小刻度10分钟,一个大刻度30分钟
        model = new SizeParam(30, 10, 60 * 1000, 12);
        sizeParamMap.put(UnitModel.UNITVALUE_10_MIN, model);
        //模式四：最小刻度30分钟,一个大刻度1小时
        model = new SizeParam(60, 30, 60 * 1000, 10);
        sizeParamMap.put(UnitModel.UNITVALUE_30_MIN, model);
        //模式五：最小刻度1小时,一个大刻度２小时
        model = new SizeParam(2 * 60, 60, 60 * 1000, 8);
        sizeParamMap.put(UnitModel.UNITVALUE_1_HOUR, model);

        //设置默认模式为　　模式二：最小刻度5分钟,一个大刻度10分钟
        currSizeParam = sizeParamMap.get(UnitModel.UNITVALUE_5_MIN);
    }*/


    public ScaleModel() {
        SizeParam model = null;
        //模式一：最小刻度1分钟,一个大刻度５分钟
        model = new SizeParam(5, 1, 60 * 1000, 16);
        //sizeParamMap.put(UnitModel.UNITVALUE_1_MIN, model);
        //模式二：最小刻度5分钟,一个大刻度10分钟
        model = new SizeParam(10, 5, 60 * 1000, 14);
        //sizeParamMap.put(UnitModel.UNITVALUE_5_MIN, model);
        //模式三：最小刻度10分钟,一个大刻度30分钟
        model = new SizeParam(100, 50, 1, 12);
        sizeParamMap.put(UnitModel.UNITVALUE_10_MIN, model);
        //模式四：最小刻度30分钟,一个大刻度1小时
        model = new SizeParam(500, 100, 1, 10);
        sizeParamMap.put(UnitModel.UNITVALUE_30_MIN, model);
        //模式五：最小刻度1小时,一个大刻度２小时
        model = new SizeParam(1000, 500, 1, 8);
        sizeParamMap.put(UnitModel.UNITVALUE_1_HOUR, model);

        //设置默认模式为　　模式二：最小刻度5分钟,一个大刻度10分钟
        currSizeParam = sizeParamMap.get(UnitModel.UNITVALUE_1_HOUR);
    }

    public void setSizeParam(UnitModel unitModle) {
        SizeParam model = sizeParamMap.get(unitModle);
        if (null == model) {
            return;
        }
        currSizeParam = model;
        setUpScaleList();
    }

    private void setUpScaleList() {
        scaleList.clear();
        Scaler scaler = null;
        for (int i = 0; i < ((getEndValue()) - getSartValue()) * 1.0f / (getUnitValue() * getDecimal()); i++) {
            scaler = new Scaler();
            //当前刻度位置(即第几个刻度)
            scaler.setPosition(i);
            //是否是关键刻度
            if(i >= 2 && i % (getLargeValue() / getUnitValue()) == 0 && scaler.getPosition() * getUnitValue() * getDecimal() % 1000 != 0){
            }else {
                scaler.setKeyScaler(i % (getLargeValue() / getUnitValue()) == 0);
            }


            scaleList.add(scaler);
        }
    }

    public int getWidthBySizeParm(UnitModel unitModel) {
        return getWidthBySizeParm(sizeParamMap.get(unitModel));
    }

    public int getWidthBySizeParm(SizeParam sizeParam) {
        if (null == sizeParam) return 0;
        // 当前模式下有几个刻度(small)
        int scaleCount = (int) ((getEndValue() - getSartValue()) / (sizeParam.getDecimal() * sizeParam.getUnitValue()));
        return (int) (scaleCount * getPixelsPerScaler(sizeParam));
    }

    public int getScaleWith() {
        return getWidthBySizeParm(currSizeParam);
    }

    public List<Scaler> getScaleList() {
        return scaleList;
    }

    public int getUnitValue() {
        return null == currSizeParam ? 0 : currSizeParam.getUnitValue();
    }

    public int getLargeValue() {
        return null == currSizeParam ? 0 : currSizeParam.getLargeValue();
    }

    public void setSartValue(long sartValue) {
        this.sartValue = sartValue;
    }

    public void setEndValue(long endValue) {
        if (null != currSizeParam) {
            this.endValue = endValue /*+ currSizeParam.getUnitValue() * currSizeParam.getDecimal()*//*补上１个刻度*/;
        }
        setUpScaleList();
    }

    public long getSartValue() {
        return sartValue;
    }

    public long getEndValue() {
        return endValue;
    }

    public int getDecimal() {
        return null == currSizeParam ? 0 : currSizeParam.getDecimal();
    }

    public float getScaleCount() {
        float scaleCount = 0;
        if (null != currSizeParam) {
            scaleCount = (getEndValue() - getSartValue()) * 1.f / (currSizeParam.getDecimal() * currSizeParam.getUnitValue());
        }
        return scaleCount;
    }

    public void setDisPlayWidth(int disPlayWidth) {
        this.disPlayWidth = disPlayWidth;
    }

    public int getDisPlayWidth() {
        return disPlayWidth;
    }

    public float getPixelsPerScaler(SizeParam sizeParam) {
        return disPlayWidth * 1.0f / (sizeParam.getLargeValue() / sizeParam.getUnitValue() * getDisPlayCount());
    }

    public int getDisPlayCount() {
        return null == currSizeParam ? 0 : currSizeParam.getDisPlayLargeCount();
    }

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");

    //计算当前刻度代表的真实意义:   (当前刻度位置) * (一个刻度表示值) + (开始刻度值)
    public String getSubscrib(Scaler scaler) {
        if (null == scaler) return null;
        try {
            //long scaleTimeMill = getSartValue() + scaler.getPosition() * getUnitValue() * getDecimal();
            //return simpleDateFormat.format(scaleTimeMill);
            long scaleTimeMill = scaler.getPosition() * getUnitValue() * getDecimal();
            return milliToMinuteTime(scaleTimeMill);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean changeSize(int scaleWidth) {
        if (getWidthBySizeParm(UnitModel.UNITVALUE_1_HOUR) <= scaleWidth
                && scaleWidth < getWidthBySizeParm(UnitModel.UNITVALUE_30_MIN)) {
            setSizeParam(UnitModel.UNITVALUE_1_HOUR);
        } else if (getWidthBySizeParm(UnitModel.UNITVALUE_30_MIN) <= scaleWidth
                && scaleWidth < getWidthBySizeParm(UnitModel.UNITVALUE_10_MIN)) {
            setSizeParam(UnitModel.UNITVALUE_30_MIN);
        } else if (getWidthBySizeParm(UnitModel.UNITVALUE_10_MIN) <= scaleWidth
                && scaleWidth < getWidthBySizeParm(UnitModel.UNITVALUE_5_MIN)) {
            setSizeParam(UnitModel.UNITVALUE_10_MIN);
        } else if (getWidthBySizeParm(UnitModel.UNITVALUE_5_MIN) <= scaleWidth
                && scaleWidth < getWidthBySizeParm(UnitModel.UNITVALUE_1_MIN)) {
            setSizeParam(UnitModel.UNITVALUE_5_MIN);
        } else if (getWidthBySizeParm(UnitModel.UNITVALUE_1_MIN) <= scaleWidth
                && scaleWidth <= getWidthBySizeParm(UnitModel.UNITVALUE_1_MIN) * 3 / 2) {
            setSizeParam(UnitModel.UNITVALUE_1_MIN);
        } else {
            return false;
        }
        return true;
    }


    /**
     * 将毫秒数转成00：00 格式
     * @param duration
     * @return
     */
    public static String milliToMinuteTime(long duration){
        String time = "" ;
        long minute = duration / 60000 ;
        long seconds = duration % 60000 ;
        long second = Math.round((float)seconds/1000) ;
        if( minute < 10 ){
            time += "0" ;
        }
        time += minute+":" ;
        if( second < 10 ){
            time += "0" ;
        }
        time += second ;
        return time ;
    }
}
