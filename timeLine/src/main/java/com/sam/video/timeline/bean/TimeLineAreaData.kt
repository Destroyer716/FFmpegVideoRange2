package com.sam.video.timeline.bean

/**
 * 时间轴区域数据
 * 实现该接口以指定时间轴上的一段区域，并在开始和结束位置与相应的视频片断绑定
 * @author SamWang(33691286@qq.com)
 * @date 2019-12-04
 */
interface TimeLineAreaData {
    var start: Long //区域开始位置
    var duration: Long //区域持续时间
    //-----------
    var startVideoClipId: String //起始点所属片段，编辑完贴纸后需记录
    var startVideoClipOffsetMs: Long //起始点位于所属片段位置的时间（未处理变速、绝对时长），仅在贴纸调整时会有影响
    var endVideoClipId: String //结束位置
    var endVideoClipOffsetMs: Long //起始点位于所属片段位置的时间（未处理变速、绝对时长）

}