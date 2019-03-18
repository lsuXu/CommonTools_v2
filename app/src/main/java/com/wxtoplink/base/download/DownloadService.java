package com.wxtoplink.base.download;

import com.wxtoplink.base.download.adapt.DownloadListenerAdapt;
import com.wxtoplink.base.download.listener.DownloadInterceptor;
import com.wxtoplink.base.download.listener.DownloadListener;
import com.wxtoplink.base.download.utils.DownloadUtil;

import java.io.InputStream;

import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;

/**
 * Created by 12852 on 2019/3/1.
 */

public final class DownloadService implements Runnable{

    private final DownloadTask downloadTask ;

    private final DownloadListener downloadListener ;

    private final Observer observer ;

    public DownloadService(DownloadTask downloadTask, Observer observer) {
        this.downloadTask = downloadTask;
        this.downloadListener = downloadTask.getDownloadListener() == null? new DownloadListenerAdapt():downloadTask.getDownloadListener();
        this.observer = observer;
    }

    public DownloadListener getDownloadListener() {
        return downloadListener;
    }

    public DownloadTask getDownloadTask() {
        return downloadTask;
    }

    @Override
    public void run() {

        downloadTask.setStatus(Status.PREPARE);
        downloadListener.onPrepareDownload(downloadTask);

        //开始的下载范围
        long rangeStart = DownloadUtil.getRangeStart(downloadTask);

        RetrofitHelper.getInstance()
                .getRetrofit(new DownloadInterceptor(downloadListener , rangeStart))
                .create(IDownload.class)
                .download("bytes=" + Long.toString(rangeStart)+ "-",downloadTask.getDownload_url())
                .map(new Function<ResponseBody, InputStream>() {
                    @Override
                    public InputStream apply(ResponseBody responseBody) throws Exception {
                        return responseBody.byteStream();
                    }
                })
                .subscribe(new Consumer<InputStream>() {
                    @Override
                    public void accept(InputStream inputStream) throws Exception {
                        //开始下载文件
                        downloadListener.onStartDownLoad();

                        downloadTask.setStatus(Status.DOWNLOADING);
                        //下载文件
                        boolean success = DownloadUtil.writeFile(inputStream,downloadTask);

                        if(success) {
                            //下载完成，执行下载完成回调
                            if (downloadTask.getMd5() != null && downloadTask.getMd5().length() > 0) {
                                if (DownloadUtil.checkMd5(downloadTask.getFile_path(), downloadTask.getMd5())) {
                                    downloadTask.setStatus(Status.DOWNLOAD_SUCCESS);
                                    downloadListener.onFinishDownload();
                                } else {
                                    downloadTask.setStatus(Status.DOWNLOAD_ERROR);
                                    downloadListener.onError(new Throwable("MD5 is mismatches"));
                                }
                            } else {
                                downloadTask.setStatus(Status.DOWNLOAD_SUCCESS);
                                downloadListener.onFinishDownload();
                            }
                        }else{
                            downloadTask.setStatus(Status.DOWNLOAD_ERROR);
                            downloadListener.onError(new Throwable("Download fail"));
                        }
                        //移除下载任务
                        observer.downloadFinish(DownloadService.this);

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        //下载出错
                        downloadTask.setStatus(Status.DOWNLOAD_ERROR);
                        downloadListener.onError(throwable);
                        observer.downloadFinish(DownloadService.this);
                    }
                });
    }

}
