package com.sam.video.timeline.common;

import com.sam.video.timeline.bean.TargetBean;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created By Ele
 * on 2020/6/4
 **/
public class TargetBeanQueue {

    private LinkedBlockingQueue<TargetBean> list = new LinkedBlockingQueue();

    public boolean enQueue(TargetBean packetBean){
        if (list != null){
            return list.offer(packetBean);
        }
        return false;
    }

    public TargetBean deQueue(){
        if (list == null){
            return null;
        }
        return list.poll();
    }

    public TargetBean getFirst(){
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
