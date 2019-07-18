package com.wxtoplink.base.log;

import android.util.Log;

/**
 * 日志输出抽象类
 * Created by 12852 on 2019/2/28.
 */

public class LogInstance {

    private LogOperate logOperate ;

    private boolean printf = true;

    LogInstance(){};

    public void setLogOperate(LogOperate logOperate) {
        this.logOperate = logOperate;
    }

    public void setPrintf(boolean printf) {
        this.printf = printf;
    }

    public void i(String tag, String msg){
        outputLog(Level.I,tag,msg);
        if(printf)
            Log.i(tag,msg);
    }

    public void i(String tag, String msg, Throwable tr){
        outputLog(Level.I,tag, msg, tr);
        if(printf) {
            Log.i(tag, msg, tr);
            tr.printStackTrace();
        }
    }

    public void e(String tag,String msg){
        outputLog(Level.E,tag,msg);
        if(printf)
            Log.e(tag,msg);
    }

    public void e(String tag, String msg, Throwable tr){
        outputLog(Level.E,tag, msg, tr);
        if(printf) {
            Log.e(tag, msg, tr);
            tr.printStackTrace();
        }
    }

    public void d(String tag,String msg){
        outputLog(Level.D,tag,msg);
        if(printf)
            Log.d(tag,msg);
    }

    public void d(String tag, String msg, Throwable tr){
        outputLog(Level.D,tag, msg, tr);
        if(printf) {
            Log.d(tag, msg, tr);
            tr.printStackTrace();
        }
    }

    public void v(String tag,String msg){
        outputLog(Level.V,tag,msg);
        if(printf)
            Log.v(tag,msg);
    }

    public void v(String tag, String msg, Throwable tr){
        outputLog(Level.V,tag, msg, tr);
        if(printf) {
            Log.v(tag, msg, tr);
            tr.printStackTrace();
        }
    }

    private void outputLog(Level level , String tar, String msg){
        if(logOperate != null){
            logOperate.operate(level.toString(), tar, msg);
        }
    }

    private void outputLog(Level level , String tar, String msg, Throwable tr){
        if(logOperate != null){
            logOperate.operate(level.toString(),tar,msg,tr);
        }
    }

    public enum Level {
        E,
        V,
        I,
        D
    }

}
