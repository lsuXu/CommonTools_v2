package com.wxtoplink.base.download.listener;

import com.wxtoplink.base.download.DownloadTask;

/**
 * 文件下载监听器
 * Created by 12852 on 2018/7/24.
 */

public interface DownloadListener {

    //准备下载
    void onPrepareDownload(DownloadTask downloadTask);

    //开始下载
    void onStartDownLoad(DownloadTask downloadTask);

    //进度
    void onProgress(long receiverSize, long totalSize, int progress);

    //下载完成
    void onFinishDownload();

    //移除队列回调，返回队列当前剩余下载任务大小
    void onRemoveQueue(int queueSize);

    //全部下载完成回调
    void onAllComplete();

    //错误回调
    void onError(Throwable throwable);
}
