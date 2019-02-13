package com.wxtoplink.base.camera;

import android.util.Log;

/**
 * Created by 12852 on 2019/2/12.
 */

public class CameraLog {

    private static LogOperate logOperate ;

    private static boolean printf = true;

    public static void setLogOperate(LogOperate logOperate) {
        CameraLog.logOperate = logOperate;
    }

    public static void setPrintf(boolean printf) {
        CameraLog.printf = printf;
    }

    public static void i(String tag, String msg){
        outputLog(FLAG.I,tag,msg);
        if(printf)
            Log.i(tag,msg);
    }

    public static void i(String tag, String msg, Throwable tr){
        outputLog(FLAG.I,tag, msg, tr);
        if(printf)
            Log.i(tag,msg,tr);
    }

    public static void e(String tag,String msg){
        outputLog(FLAG.E,tag,msg);
        if(printf)
            Log.e(tag,msg);
    }

    public static void e(String tag, String msg, Throwable tr){
        outputLog(FLAG.E,tag, msg, tr);
        if(printf)
            Log.e(tag,msg,tr);
    }

    public static void d(String tag,String msg){
        outputLog(FLAG.D,tag,msg);
        if(printf)
            Log.d(tag,msg);
    }

    public static void d(String tag, String msg, Throwable tr){
        outputLog(FLAG.D,tag, msg, tr);
        if(printf)
            Log.d(tag,msg,tr);
    }

    public static void v(String tag,String msg){
        outputLog(FLAG.V,tag,msg);
        if(printf)
            Log.v(tag,msg);
    }

    public static void v(String tag, String msg, Throwable tr){
        outputLog(FLAG.V,tag, msg, tr);
        if(printf)
            Log.v(tag,msg,tr);
    }

    private static void outputLog(FLAG flag ,String tar, String msg){
        if(logOperate != null){
            logOperate.operate(flag, tar, msg);
        }
    }

    private static void outputLog(FLAG flag ,String tar, String msg, Throwable tr){
        if(logOperate != null){
            logOperate.operate(flag,tar,msg,tr);
        }
    }

    public enum FLAG{
        E,
        V,
        I,
        D
    }

    public interface LogOperate{

        void operate(FLAG flag , String tar ,String msg ,Throwable tr);

        void operate(FLAG flag , String tar ,String msg );

    }
}
