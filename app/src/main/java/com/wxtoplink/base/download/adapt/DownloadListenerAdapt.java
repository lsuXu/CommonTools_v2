package com.wxtoplink.base.download.adapt;

import com.wxtoplink.base.download.DownloadTask;
import com.wxtoplink.base.download.listener.DownloadListener;

/**
 * Created by 12852 on 2018/11/28.
 */

public class DownloadListenerAdapt implements DownloadListener{
    @Override
    public void onPrepareDownload(DownloadTask downloadTask) {

    }

    @Override
    public void onStartDownLoad() {

    }

    @Override
    public void onProgress(long receiverSize, long totalSize, int progress) {

    }

    @Override
    public void onFinishDownload() {

    }

    @Override
    public void onRemoveQueue(int queueSize) {

    }

    @Override
    public void onAllComplete() {

    }

    @Override
    public void onError(Throwable throwable) {

    }
}
