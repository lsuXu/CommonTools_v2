package com.wxtoplink.base.log;

/**
 * Created by 12852 on 2019/2/28.
 */

public class LinuxLog extends AbstractLog{

    private LinuxLog(){};

    public static LinuxLog getInstance(){
        return LogHolder.instance ;
    }

    private static final class LogHolder{
        public static final LinuxLog instance = new LinuxLog();
    }
}
