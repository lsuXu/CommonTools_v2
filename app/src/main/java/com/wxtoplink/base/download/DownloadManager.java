package com.wxtoplink.base.download;

import com.wxtoplink.base.download.listener.DownloadListener;
import com.wxtoplink.base.log.AbstractLog;
import com.wxtoplink.base.log.DownloadLog;
import com.wxtoplink.base.log.LogOperate;
import com.wxtoplink.base.log.LogOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 文件下载管理器（以下载队列方式，仅允许单个文件同时下载）
 * Created by 12852 on 2018/7/24.
 */

public final class DownloadManager implements LogOutput,Observer{

    private final AbstractLog logInstance ;

    private final String TAG ;

    //下载队列
    private List<DownloadTask> downloadQueue ;

    private List<DownloadService> downloadServiceList ;

    //下载线层，实际的下载文件的线层
    private ExecutorService downloadExecutors ;

    //等待线层,对下载任务进行增删的管理
    private ExecutorService waitExecutor ;

    //默认允许的同时下载数量
    private final int DEFAULT_SIZE = 3 ;


    public static DownloadManager getInstance (){
        return DownloadManagerHolder.instance;
    };

    private DownloadManager(){
        TAG = DownloadManager.class.getSimpleName();
        downloadQueue = new ArrayList<>();
        downloadServiceList = new ArrayList<>(DEFAULT_SIZE);
        downloadExecutors = Executors.newFixedThreadPool(DEFAULT_SIZE);
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

    public List<DownloadService> getDownloadServiceList(){
        return downloadServiceList ;
    }

    //下载第一步:当下载队列发生改变时执行，判断当前下载状况，决定是否进行文件下载
    private void download(){
        if(!downloadQueue.isEmpty() && downloadServiceList.size()< 3){
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
                }else if(!isContains(downloadTask)){
                    logInstance.i(TAG,"Add tasks to wait queue:downloadTask =" + downloadTask.toString());
                    downloadQueue.add(downloadTask);
                }else {
                    logInstance.i(TAG, "The task already exists:" + downloadTask.toString());
                }
                download();
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
                downloadTask.getDownloadListener().onRemoveQueue(downloadQueue.size());

                //如果全部下载完成，执行完成回调
                if(!hasNext()){
                    downloadTask.getDownloadListener().onAllComplete();
                }else{
                    download();
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
    private void startDownload(DownloadTask downloadTask){
        final DownloadService downloadService = new DownloadService(downloadTask,this);

        downloadExecutors.execute(new Runnable() {
            @Override
            public void run() {
                downloadServiceList.add(downloadService);
                downloadService.run();
            }
        });
    }

    @Override
    public void setLogOutput(LogOperate operate, boolean printf) {
        DownloadLog.getInstance().setLogOperate(operate);
        DownloadLog.getInstance().setPrintf(printf);
    }

    @Override
    public void downloadFinish(final DownloadService downloadService) {
        downloadExecutors.execute(new Runnable() {
            @Override
            public void run() {
                downloadServiceList.remove(downloadService);
                removeDownloadTask(downloadService.getDownloadTask());
            }
        });

    }

    private static final class DownloadManagerHolder{
        static final DownloadManager instance = new DownloadManager();
    }

}
