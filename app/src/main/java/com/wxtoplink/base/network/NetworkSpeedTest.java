package com.wxtoplink.base.network;

import android.net.TrafficStats;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by 12852 on 2019/3/20.
 */

public class NetworkSpeedTest {

    private volatile boolean interruptFlag = false ;

    private String urlString = "https://drcilabo.geniusshelf.com/media/resource/201902/151118/transparent_video_7.mp4";

    private URL url ;

    private InputStream inputStream ;//数据输入流，来自网络

    private Disposable timerDisposable ;//时间计时器，用于计算测试时间

    private Disposable downloadDisposable ;//下载线层，处理下载

    private long totalSize ,rxStartSize ,rxEndSize;//下载总大小，下载开始时设备共使用流量，下载结束时设备共使用流量

    private RateCallBack rateCallBack ;//速率回调

    //准备工作，获取连接并准备好数据流
    private void prepare() throws IOException {
        url = new URL(urlString);
        URLConnection urlConnection = url.openConnection();
        urlConnection.connect();
        inputStream = urlConnection.getInputStream();
    }

    public void start(long interval ,TimeUnit unit , RateCallBack rateCallBack) throws IOException {
        this.rateCallBack = rateCallBack ;
        rxStartSize = getTotalRxBytes();
        startTimer(interval,unit);
        startDownload();
    }

    private void startDownload() throws IOException {
        downloadDisposable = Observable.just(urlString)
                .observeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<InputStream>>() {
                    @Override
                    public ObservableSource<InputStream> apply(String s) throws Exception {
                        prepare();
                        return Observable.just(inputStream);
                    }
                })
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<InputStream>() {
                    @Override
                    public void accept(InputStream inputStream) throws Exception {

                        byte[] bytes = new byte[1024];
                        for(int len ; (len = inputStream.read(bytes))>=0;){
                            totalSize = totalSize + len ;
                            if(interruptFlag){
                                break;
                            }
                        }
                    }
                });
    }

    public void reset() throws IOException {
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
            inputStream.close();
            inputStream = null ;
        }
        interruptFlag = false ;
        totalSize = 0 ;
    }

    private void startTimer(final long interval , final TimeUnit unit){
        timerDisposable = Observable.timer(interval, unit)
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        interruptFlag = true ;

                        rxEndSize = getTotalRxBytes();
                        rateCallBack.rate(totalSize ,(rxEndSize - rxStartSize) , interval , unit );
                    }
                });
    }

    //获取当前设备总数据接收量
    private long getTotalRxBytes(){
        return TrafficStats.getTotalRxBytes();
    }

}
