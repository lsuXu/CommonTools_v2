package com.wxtoplink.base.download.listener;

import com.wxtoplink.base.download.DownloadTask;

/**
 * 文件下载监听器，一次只允许单个文件进行下载，协助控制同时下载的文件个数
 * Created by 12852 on 2018/7/24.
 */

public class DownloadListenerImpl implements DownloadListener {

    private DownloadListener downloadListener ;

    private static final String TAG = DownloadListenerImpl.class.getSimpleName();

    private static final DownloadListenerImpl instance = new DownloadListenerImpl();

    private DownloadListenerImpl(){}

    public static DownloadListenerImpl getInstance(){
        return instance ;
    }

    private boolean isDownloading = false;

    @Override
    public void onPrepareDownload(DownloadTask downloadTask) {
        isDownloading = true ;
        if(downloadListener != null){
            downloadListener.onPrepareDownload(downloadTask);
        }
    }

    @Override
    public void onStartDownLoad() {
        if(downloadListener != null){
            downloadListener.onStartDownLoad();
        }
    }

    @Override
    public void onProgress(long receiverSize , long totalSize ,int progress) {
        if(downloadListener != null){
            downloadListener.onProgress(receiverSize,totalSize,progress);
        }
    }

    @Override
    public void onFinishDownload() {
        isDownloading = false ;
        if(downloadListener!= null){
            downloadListener.onFinishDownload();
        }
    }

    @Override
    public void onRemoveQueue(int queueSize) {
        if(downloadListener != null){
            downloadListener.onRemoveQueue(queueSize);
        }
    }

    @Override
    public void onAllComplete() {
        if(downloadListener != null){
            downloadListener.onAllComplete();
            downloadListener = null ;
        }
    }

    @Override
    public void onError(Throwable throwable) {
        isDownloading = false ;
        if(downloadListener != null){
            downloadListener.onError(throwable);
        }
    }

    public boolean isDownloading(){
        return isDownloading ;
    }

    public DownloadListener getDownloadListener() {
        return downloadListener;
    }

    public void setDownloadListener(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }
}
