package com.wxtoplink.base.log;

import java.util.Hashtable;

/**
 * 日志实例构建器，用于保持日志对象的唯一
 * Created by 12852 on 2019/7/8.
 */

public class LogBuilder {

    //多线程安全，使用Hashtable
    private Hashtable<Object,LogInstance> logTable ;

    private LogBuilder() {
        logTable = new Hashtable<Object,LogInstance>() ;
    }

    public static LogBuilder getInstance(){
        return LogBuilderHolder.instance ;
    }

    /**
     * 根据传入的日志类型，构建AbstractLog对象，通过该方法构建的AbstractLog，会被复用，即每个LogType，只会对应一个AbstractLog
     * @param logType log type
     * @return  可被复用的 LogInstance 实例
     */
    public LogInstance build(LogType logType){
        if(!logTable.containsKey(logType)){
            LogInstance logInstance = new LogInstance() ;
            logTable.put(logType,logInstance);
        }
        return logTable.get(logType);
    }

    /**
     * 根据传入的key，构建AbstractLog对象，通过该方法构建的AbstractLog，会被复用，即每个唯一的key，管理一个AbstractLog
     * @param key key
     * @return  可被复用的 LogInstance 实例
     */
    public LogInstance build(Object key){
        if(!logTable.containsKey(key)){
            LogInstance logInstance = new LogInstance() ;
            logTable.put(key,logInstance);
        }
        return logTable.get(key);
    }


    /**
     * 直接构建一个AbstractLog对象,不会被复用
     * @return 不可复用的AbstractLog实例
     */
    public LogInstance build(){
        return new LogInstance() ;
    }

    //懒加载，JVM保证类加载的互斥性
    private static final class LogBuilderHolder{
        private static final LogBuilder instance = new LogBuilder();
    }

    public enum LogType{
        CAMERA,//相机日志类型
        DOWNLOAD,//下载日志类型，记录下载错误情况
        LINUX//linux日志类型
    }
}
