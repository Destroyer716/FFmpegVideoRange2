package com.example.ffmpegvideorange2;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created By Ele
 * on 2020/6/4
 **/
public class YuvQueue {

    private LinkedBlockingQueue<YUVDataBean> list = new LinkedBlockingQueue();

    public boolean enQueue(YUVDataBean packetBean){
        if (list != null){
            return list.offer(packetBean);
        }
        return false;
    }

    public YUVDataBean deQueue(){
        if (list == null){
            return null;
        }
        return list.poll();
    }

    public YUVDataBean getFirst(){
        if (list == null){
            return null;
        }
        return list.peek();
    }

    public int getQueueSize(){
        if (list == null){
            return 0;
        }
        return list.size();
    }

    public synchronized void clear(){

        if (list == null || list.isEmpty()){
            return;
        }
        list.clear();
    }

}
