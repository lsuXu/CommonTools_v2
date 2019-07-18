package com.wxtoplink.base.log;

/**
 * 日志操作接口
 * Created by 12852 on 2019/2/28.
 */

public interface LogOperate {

    void operate(String level , String tar ,String msg ,Throwable tr);

    void operate(String level , String tar ,String msg );
}
