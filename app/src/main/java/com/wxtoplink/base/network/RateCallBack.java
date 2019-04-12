package com.wxtoplink.base.network;

import java.util.concurrent.TimeUnit;

/**
 * Created by 12852 on 2019/3/21.
 */

public interface RateCallBack {


    /**
     * 测试速率回调，流量消耗数会大于下载流量数
     * @param rxBytes 下载接收到的byte
     * @param netBytes  期间流量消耗byte
     * @param interval  耗费时间
     * @param unit  时间单位
     */
    void rate(long rxBytes , long netBytes ,long interval ,TimeUnit unit);

    void onError(Throwable throwable);

}
