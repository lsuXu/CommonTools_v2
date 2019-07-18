package com.wxtoplink.base.log;

/**
 * 日志输出接口，需要输出日志的模块，需要实现该接口
 * Created by 12852 on 2019/2/28.
 */

public interface LogOutput {

    void setLogOutput(LogOperate operate , boolean printf);
}
