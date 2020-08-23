package com.example.myplayer;

import com.example.myplayer.bean.YUVBean;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created By Ele
 * on 2020/6/4
 **/
public class MyQueue {

    private LinkedBlockingQueue<YUVBean> list = new LinkedBlockingQueue();

    public boolean enQueue(YUVBean yuvBean){
        if (list != null){
            return list.offer(yuvBean);
        }
        return false;
    }

    public YUVBean deQueue(){
        if (list == null){
            return null;
        }
        return list.poll();
    }

    public YUVBean getFirst(){
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

}
