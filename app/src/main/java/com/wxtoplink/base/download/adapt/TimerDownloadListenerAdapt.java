package com.wxtoplink.base.download.adapt;

import com.wxtoplink.base.download.DownloadTask;
import com.wxtoplink.base.download.listener.DownloadListener;
import com.wxtoplink.base.download.listener.TimerProgress;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * 下载监听器的适配类，提供定时回传进度功能
 * Created by 12852 on 2019/3/18.
 */

public abstract class TimerDownloadListenerAdapt implements DownloadListener,TimerProgress {
    private DownloadTask downloadTask ;//下载任务

    private Disposable timer ;//定时器

    private boolean timerProgressFlag = false ;//定时进度回调标志

    private int period ;//定时周期，单位为秒

    private TimeUnit timeUnit ;//时间单位，默认为秒

    public TimerDownloadListenerAdapt(int secondPeriod) {
        this(secondPeriod , TimeUnit.SECONDS);
    }

    public TimerDownloadListenerAdapt(int period , TimeUnit timeUnit){
        this.period = period ;
        this.timeUnit = timeUnit ;
    }

    public DownloadTask getDownloadTask() {
        return downloadTask;
    }

    @Override
    public void onPrepareDownload(DownloadTask downloadTask) {
        this.downloadTask = downloadTask ;
        timer = Observable.interval(0,period, timeUnit)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        timerProgressFlag = true ;
                    }
                });
    }

    @Override
    public void onRemoveQueue(int queueSize) {
        release();//移除队列时，关闭定时器
    }

    @Override
    public void onProgress(long receiverSize, long totalSize, int progress) {
        //若五秒周期到，或者下载文件大小等于文件总大小
        if(timerProgressFlag || receiverSize == totalSize) {
            timerProgressFlag = false;
            timerProgress(receiverSize,totalSize,progress);
        }

    }

    @Override
    public void onStartDownLoad() {

    }

    @Override
    public void onFinishDownload() {

    }

    @Override
    public void onError(Throwable throwable) {

    }

    private void release(){
        if(timer != null && !timer.isDisposed()){
            timer.dispose();
            timer = null ;
        }
    }
}
