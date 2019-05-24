package com.wxtoplink.base.network;

import android.net.TrafficStats;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * 网速测试工具类
 * Created by 12852 on 2019/3/20.
 */

public class NetworkSpeedTest {

    private volatile boolean interruptFlag = true , cancelFlag = false;

    //测试使用url
    private String url = "http://testjnj.geniusshelf.com/media/file/version/201903/TeamViewerHost.apk";

    private URL mUrl ;

    private InputStream inputStream ;//数据输入流，来自网络

    private Disposable timerDisposable ;//时间计时器，用于计算测试时间

    private Disposable downloadDisposable ;//下载线层，处理下载

    private volatile long  rxStartSize ,rxEndSize;//下载开始时设备共使用流量，下载结束时设备共使用流量

    private volatile long downTotalSize ,downStartTime,downEndTime ;//下载总大小,下载开始时间和结束时间

    private RateCallBack rateCallBack ;//速率回调

    //准备工作，获取连接并准备好数据流
    private InputStream getStream(String url) throws IOException {
        mUrl = new URL(url);
        URLConnection urlConnection = mUrl.openConnection();
        urlConnection.connect();
        inputStream = urlConnection.getInputStream();
        return inputStream ;
    }

    public void start(long interval ,TimeUnit unit , RateCallBack rateCallBack) {
        this.rateCallBack = rateCallBack ;
        this.interruptFlag = false ;
        this.cancelFlag = false ;
        startTimer(interval,unit);
        startDownload();
    }

    public void setUrl(String url) {
        this.url = url;
    }

    //开始下载
    private void startDownload() {
        downloadDisposable = Observable.just(url)
                .observeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<InputStream>>() {
                    @Override
                    public ObservableSource<InputStream> apply(String url) throws Exception {
                        return Observable.just(getStream(url));
                    }
                })
                .unsubscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<InputStream>() {
                    @Override
                    public void accept(InputStream inputStream) throws Exception {

                        //记录开始
                        recordStart();

                        byte[] bytes = new byte[1024];
                        for (int len; (len = inputStream.read(bytes)) >= 0; ) {
                            if (interruptFlag && cancelFlag) {
                                break;
                            }
                            downTotalSize = downTotalSize + len;
                        }
                        //关闭流
                        inputStream.close();

                        if(!cancelFlag) {
                            //不是取消，则记录结束时间
                            recordEnd();
                        }

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        //发生错误，重置状态，并给出错误回调
                        cancel();
                        rateCallBack.onError(throwable);
                    }
                });
    }

    //记录开始下载时的数据信息
    private void recordStart(){
        //开始下载时间
        downStartTime = new Date().getTime() ;
        //开始时网络流量使用大小
        rxStartSize = getTotalRxBytes() ;
    }

    //记录下载停止时的数据信息
    private void recordEnd(){
        if(downEndTime == 0){
            //结束时间
            downEndTime = new Date().getTime();
        }
        if(rxEndSize == 0){
            //结束时流量总共使用大小
            rxEndSize = getTotalRxBytes();
        }
    }

    //取消测速
    public void cancel() {
        cancelFlag = true ;
        interruptFlag = true ;
        //定时器取消订阅
        if(timerDisposable != null && !timerDisposable.isDisposed()){
            timerDisposable.dispose();
            timerDisposable = null ;
        }
        //停止下载线层
        if(downloadDisposable != null && !downloadDisposable.isDisposed()){
            downloadDisposable.dispose();
            downloadDisposable = null ;
        }
        if(inputStream != null){
            inputStream = null ;
        }
        downTotalSize = 0 ;
        downStartTime = 0 ;
        downEndTime = 0 ;
        rxStartSize = 0 ;
        rxEndSize = 0 ;
    }


    //开启定时器，时间单位越小，精度越高
    private void startTimer(final long interval , final TimeUnit unit){
        timerDisposable = Observable.timer(interval, unit)
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        interruptFlag = true ;
                        //记录结束
                        recordEnd();
                        rateCallBack.rate(downTotalSize ,(rxEndSize - rxStartSize) ,unit.convert(downEndTime - downStartTime ,TimeUnit.MILLISECONDS) , unit );
                        cancel();//给出回调后，重置
                    }
                });
    }

    //获取当前设备总数据接收量
    private long getTotalRxBytes(){
        return TrafficStats.getTotalRxBytes();
    }

}
