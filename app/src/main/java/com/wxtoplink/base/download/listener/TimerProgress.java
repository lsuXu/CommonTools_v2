package com.wxtoplink.base.download.listener;

/**
 * Created by 12852 on 2019/3/18.
 */

public interface TimerProgress {

    //按照一定周期返回下载进度
    public void timerProgress(long receiverSize, long totalSize, int progress);

}
