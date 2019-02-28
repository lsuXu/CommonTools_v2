package com.wxtoplink.base.log;

/**
 * Created by 12852 on 2019/2/28.
 */

public class DownloadLog extends AbstractLog{

    private DownloadLog(){};

    public static DownloadLog getInstance(){
        return LogHolder.instance ;
    }

    private static final class LogHolder{
        public static final DownloadLog instance = new DownloadLog();
    }

}
