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
    private List<DownloadTask> downloadQueue ;//由waitExecutor线层负责操作，其他线层禁止操作

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
        downloadExecutors = Executors.newFixedThreadPool(DEFAULT_SIZE);
        waitExecutor = Executors.newSingleThreadExecutor();
        logInstance = DownloadLog.getInstance();
    }

    //添加下载任务
    public DownloadManager downloadFile(String url ,String fileName,String filePath){
        return downloadFile(url,fileName,filePath,null);
    }

    /**
     * 添加下载任务
     * @param filePath 文件保存全路径
     * @param url   网络下载路径
     * @param downloadListener 网络回调
     * @return
     */
    public DownloadManager downloadFile(String url,String fileName,String filePath, DownloadListener downloadListener){
        DownloadTask downloadObject = new DownloadTask(url,fileName,filePath,null,downloadListener);
        return addDownloadTask(downloadObject,true);
    }

    public DownloadManager downloadFile(DownloadTask downloadTask){
        return addDownloadTask(downloadTask,true);
    }

    public DownloadManager downloadFile(DownloadTask downloadTask,boolean noRepeat){
        return addDownloadTask(downloadTask,noRepeat);
    }

    public List<DownloadTask> getDownloadQueue(){
        return downloadQueue;
    }


    //添加下载任务
    private DownloadManager addDownloadTask(final DownloadTask downloadTask,final boolean noRepeat){
        waitExecutor.execute(new Runnable() {
            @Override
            public void run() {
                //不需要校验，下载队列为空或者下载队列中不包含当前任务,添加下载任务
                if(!noRepeat || downloadQueue.isEmpty() || !isContains(downloadTask)){
                    downloadQueue.add(downloadTask);
                    downloadTask.setStatus(Status.PREPARE);
                    prepareDownload(downloadTask);
                }
            }
        });
        return this;
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


    private void prepareDownload(DownloadTask downloadTask , Observer observer){
        DownloadService downloadService = new DownloadService(downloadTask,observer);
        downloadExecutors.execute(downloadService);
    }

    //实际的下载方式
    private void prepareDownload(DownloadTask downloadTask){
        prepareDownload(downloadTask,this);
    }

    @Override
    public void setLogOutput(LogOperate operate, boolean printf) {
        DownloadLog.getInstance().setLogOperate(operate);
        DownloadLog.getInstance().setPrintf(printf);
    }

    @Override
    public void downloadFinish(final DownloadService downloadService) {
        removeDownloadTask(downloadService.getDownloadTask());
    }

    public void downloadGroup(Group group){
        group.notifyStart();
        for(DownloadTask downloadTask : group.getWaitTasks()){
            if(downloadTask.getStatus() != Status.PREPARE.getId()){
                downloadTask.setStatus(Status.PREPARE);//修改任务状态，避免重复添加到下载队列中
                prepareDownload(downloadTask,group.getGroupObserver());
            }
        }
    }

    private static final class DownloadManagerHolder{
        static final DownloadManager instance = new DownloadManager();
    }

}
