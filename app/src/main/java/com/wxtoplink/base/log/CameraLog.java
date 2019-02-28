package com.wxtoplink.base.log;

/**
 * Created by 12852 on 2019/2/28.
 */

public class CameraLog extends AbstractLog{

    private CameraLog(){};

    public static CameraLog getInstance(){
        return LogHolder.instance ;
    }

    private static final class LogHolder{
        public static final CameraLog instance = new CameraLog();
    }
}
