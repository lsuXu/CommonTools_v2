package com.wxtoplink.base.download.listener;

import com.wxtoplink.base.download.DownloadTask;

/**
 * 文件下载监听器
 * Created by 12852 on 2018/7/24.
 */

public interface DownloadListener {

    void onStartDownLoad(DownloadTask downloadTask);

    void onProgress(long receiverSize, long totalSize, int progress);

    void onFinishDownload();

    void onError(String errorMessage);
}
