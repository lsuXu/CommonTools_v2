package com.wxtoplink.base.download;

import com.wxtoplink.base.download.listener.DownloadListener;
import com.wxtoplink.base.download.listener.DownloadListenerImpl;
import com.wxtoplink.base.download.utils.DownloadUtil;
import com.wxtoplink.base.log.AbstractLog;
import com.wxtoplink.base.log.DownloadLog;
import com.wxtoplink.base.log.LogOperate;
import com.wxtoplink.base.log.LogOutput;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;

/**
 * 文件下载管理器（以下载队列方式，仅允许单个文件同时下载）
 * Created by 12852 on 2018/7/24.
 */

public final class DownloadManager implements LogOutput{

    private final AbstractLog logInstance ;

    private final String TAG ;

    //下载队列
    private List<DownloadTask> downloadQueue ;

    //下载线层，实际的下载文件的线层
    private ExecutorService downloadExecutors ;

    //等待线层,对下载任务进行增删的管理
    private ExecutorService waitExecutor ;


    public static DownloadManager getInstance (){
        return DownloadManagerHolder.instance;
    };

    private DownloadManager(){
        TAG = DownloadManager.class.getSimpleName();
        downloadQueue = new ArrayList<>();
        downloadExecutors = Executors.newSingleThreadExecutor();
        waitExecutor = Executors.newSingleThreadExecutor();
        logInstance = DownloadLog.getInstance();
    }

    //添加下载任务
    public void downloadFile(String url ,String fileName,String filePath){
        downloadFile(url,fileName,filePath,null);
    }

    /**
     * 添加下载任务
     * @param filePath 文件保存全路径
     * @param url   网络下载路径
     * @param downloadListener 网络回调
     * @return
     */
    public void downloadFile(String url,String fileName,String filePath, DownloadListener downloadListener){
        DownloadTask downloadObject = new DownloadTask(url,fileName,filePath,null,downloadListener);
        addDownloadTask(downloadObject,true);
    }

    public void downloadFile(DownloadTask downloadTask){
        addDownloadTask(downloadTask,true);
    }

    public void downloadFile(DownloadTask downloadTask,boolean noRepeat){
        addDownloadTask(downloadTask,noRepeat);
    }

    public List<DownloadTask> getDownloadQueue(){
        return downloadQueue;
    }

    //下载第一步:当下载队列发生改变时执行，判断当前下载状况，决定是否进行文件下载
    private void download(){
        if(!downloadQueue.isEmpty() && !DownloadListenerImpl.getInstance().isDownloading()){
            for(DownloadTask downloadTask: downloadQueue){
                if(downloadTask.getStatus() == Status.ORIGINAL.getId()){//查找初始状态的任务，进行下载
                    downloadTask.setStatus(Status.PREPARE);//修改任务状态，避免重复添加到下载队列中
                    startDownload(downloadTask);
                    break;
                }
            }
        }
    }

    //添加下载任务
    private void addDownloadTask(final DownloadTask downloadTask,final boolean noRepeat){
        waitExecutor.execute(new Runnable() {
            @Override
            public void run() {
                //下载队列为空或者下载队列中不包含当前任务,添加下载任务
                if(!noRepeat || downloadQueue.isEmpty()){
                    logInstance.i(TAG,"Add tasks to wait queue:downloadTask =" + downloadTask.toString());
                    downloadQueue.add(downloadTask);
                    download();
                }else if(!isContains(downloadTask)){
                    logInstance.i(TAG,"Add tasks to wait queue:downloadTask =" + downloadTask.toString());
                    downloadQueue.add(downloadTask);
                    download();
                }else {
                    logInstance.i(TAG, "The task already exists:" + downloadTask.toString());
                }
            }
        });
    };

    //移除下载任务
    private void removeDownloadTask(final DownloadTask downloadTask){
        waitExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if(downloadQueue.contains(downloadTask)){
                    logInstance.i(TAG,"Delete task:" + downloadTask.toString());
                    downloadQueue.remove(downloadTask);
                }
                //移除下载任务回调
                DownloadListenerImpl.getInstance().onRemoveQueue(downloadQueue.size());

                download();

                //如果全部下载完成，执行完成回调
                if(!hasNext()){
                    DownloadListenerImpl.getInstance().onAllComplete();
                }
            }
        });
    }

    //是否已经存在这个任务
    private boolean isContains(DownloadTask downloadTask){
        if(downloadQueue.contains(downloadTask)){
            return true ;
        }
        for(DownloadTask task:downloadQueue){
            if(task.getFile_path().equals(downloadTask.getFile_path())){
                return true ;
            }
        }
        return false;
    }


    private boolean hasNext(){
        return !downloadQueue.isEmpty();
    }

    //实际的下载方式
    private void startDownload(final DownloadTask downloadTask){
        logInstance.i(TAG,String.format("Add tasks to the download queue:%s",downloadTask.toString()));
        downloadExecutors.execute(new Runnable() {
            @Override
            public void run() {
                logInstance.i(TAG,"prepare download:" + downloadTask.toString());
                File file = new File(downloadTask.getFile_path());
                if(file.exists()){
                    file.delete();
                }

                //设置下载监听器
                DownloadListenerImpl.getInstance().setDownloadListener(downloadTask.getDownloadListener());

                //准备下载文件
                DownloadListenerImpl.getInstance().onPrepareDownload(downloadTask);

                RetrofitHelper.getInstance()
                        .getRetrofit()
                        .create(DownloadService.class)
                        .download("bytes=" + Long.toString(DownloadUtil.getRangeStart(downloadTask))+ "-",downloadTask.getDownload_url())
                        .map(new Function<ResponseBody, InputStream>() {
                            @Override
                            public InputStream apply(ResponseBody responseBody) throws Exception {
                                return responseBody.byteStream();
                            }
                        })
                        .subscribe(new Consumer<InputStream>() {
                            @Override
                            public void accept(InputStream inputStream) throws Exception {
                                logInstance.i(TAG,"start download:" + downloadTask.toString());
                                //开始下载文件
                                DownloadListenerImpl.getInstance().onStartDownLoad();

                                downloadTask.setStatus(Status.DOWNLOADING);
                                //下载文件
                                boolean success = DownloadUtil.writeFile(inputStream,downloadTask);

                                if(success) {
                                    //下载完成，执行下载完成回调
                                    if (downloadTask.getMd5() != null && downloadTask.getMd5().length() > 0) {
                                        if (DownloadUtil.checkMd5(downloadTask.getFile_path(), downloadTask.getMd5())) {
                                            downloadTask.setStatus(Status.DOWNLOAD_SUCCESS);
                                            logInstance.i(TAG, "download finish:" + downloadTask.toString());
                                            DownloadListenerImpl.getInstance().onFinishDownload();
                                        } else {
                                            downloadTask.setStatus(Status.DOWNLOAD_ERROR);
                                            logInstance.i(TAG, "download error:MD5 is mismatches" + downloadTask.toString());
                                            DownloadListenerImpl.getInstance().onError(new Throwable("MD5 is mismatches"));
                                        }
                                    } else {
                                        downloadTask.setStatus(Status.DOWNLOAD_SUCCESS);
                                        logInstance.i(TAG, "download finish:" + downloadTask.toString());
                                        DownloadListenerImpl.getInstance().onFinishDownload();
                                    }
                                }else{
                                    downloadTask.setStatus(Status.DOWNLOAD_ERROR);
                                    logInstance.i(TAG, String.format("download error:write stream to file error,%s",downloadTask.toString()));
                                    DownloadListenerImpl.getInstance().onError(new Throwable("Download fail"));
                                }
                                //移除下载任务
                                removeDownloadTask(downloadTask);

                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                logInstance.e(TAG,"download error:" + downloadTask.toString(),throwable);
                                //下载出错
                                downloadTask.setStatus(Status.DOWNLOAD_ERROR);
                                DownloadListenerImpl.getInstance().onError(throwable);
                                //移除下载任务，移除后DownloadListenerImpl.getInstance()中的监听对象会被置空
                                removeDownloadTask(downloadTask);

                            }
                        });
            }
        });

    }

    @Override
    public void setLogOutput(LogOperate operate, boolean printf) {
        DownloadLog.getInstance().setLogOperate(operate);
        DownloadLog.getInstance().setPrintf(printf);
    }

    private static final class DownloadManagerHolder{
        static final DownloadManager instance = new DownloadManager();
    }

}
