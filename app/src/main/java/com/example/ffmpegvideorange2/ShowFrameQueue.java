package com.example.ffmpegvideorange2;

import com.example.myplayer.PacketBean;
import com.sam.video.timeline.bean.TargetBean;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created By Ele
 * on 2020/6/4
 **/
public class ShowFrameQueue {

    private LinkedBlockingQueue<ImageViewBitmapBean> list = new LinkedBlockingQueue();

    public boolean enQueue(ImageViewBitmapBean packetBean){
        if (list != null){
            return list.offer(packetBean);
        }
        return false;
    }

    public ImageViewBitmapBean deQueue(){
        if (list == null){
            return null;
        }
        return list.poll();
    }

    public ImageViewBitmapBean getFirst(){
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
