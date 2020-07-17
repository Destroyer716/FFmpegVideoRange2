package com.example.myplayer.mediacodec;

import android.media.MediaCodecList;

import java.util.HashMap;
import java.util.Map;

/**
 * Created By Ele
 * on 2020/5/31
 **/
public class KzglVideoSupportUtil {

    private static Map<String, String> codecMap = new HashMap<>();
    //ffmpeg中的编码格式与mediaCodec 中解码器名称的一一对应关系
    static {
        codecMap.put("h264", "video/avc");
    }

    public static String findVideoCodecName(String ffcodename)
    {
        if(codecMap.containsKey(ffcodename))
        {
            return codecMap.get(ffcodename);
        }
        return "";
    }

    public static boolean isSupportCodec(String ffcodecname)
    {
        boolean supportvideo = false;
        int count = MediaCodecList.getCodecCount();
        for(int i = 0; i < count; i++)
        {
            String[] tyeps = MediaCodecList.getCodecInfoAt(i).getSupportedTypes();
            for(int j = 0; j < tyeps.length; j++)
            {
                if(tyeps[j].equals(findVideoCodecName(ffcodecname)))
                {
                    supportvideo = true;
                    break;
                }
            }
            if(supportvideo)
            {
                break;
            }
        }
        return supportvideo;
    }
}
