package com.wxtoplink.base.download;

import android.util.Log;

import com.wxtoplink.base.download.listener.DownloadListener;
import com.wxtoplink.base.download.listener.DownloadListenerImpl;
import com.wxtoplink.base.tools.EncryptionCheckUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class DownloadManager {

    private static final String TAG = DownloadManager.class.getSimpleName();

    private static final DownloadManager instance = new DownloadManager();
    //下载队列
    private List<DownloadTask> downloadQueue ;

    //下载线层，实际的下载文件的线层
    private ExecutorService downloadExecutors ;

    //等待线层,对下载任务进行增删的管理
    private ExecutorService waitExecutor ;


    public static DownloadManager getInstance (){
        return instance;
    };

    private DownloadManager(){
        downloadQueue = new ArrayList<>();
        downloadExecutors = Executors.newSingleThreadExecutor();
        waitExecutor = Executors.newSingleThreadExecutor();
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

    //下载第一步：当下载队列发生改变时执行，判断当前下载状况，决定是否进行文件下载
    private void download(){

        if(!downloadQueue.isEmpty() && !DownloadListenerImpl.getInstance().isDownloading()){
            startDownload(downloadQueue.get(0));
        }else if(DownloadListenerImpl.getInstance().isDownloading()){

        }else{
            Log.i(TAG,"任务已经全部下载完成");
        }

    }

    //添加下载任务
    private void addDownloadTask(final DownloadTask downloadTask,final boolean noRepeat){
        waitExecutor.execute(new Runnable() {
            @Override
            public void run() {
                //下载队列为空或者下载队列中不包含当前任务,添加下载任务
                if(!noRepeat || downloadQueue.isEmpty()){
                    Log.i(TAG,"添加下载任务：hashCode =" + downloadTask.hashCode() + "  ;地址：" + downloadTask);
                    downloadQueue.add(downloadTask);
                    download();
                }else if(!isContains(downloadTask)){
                    Log.i(TAG,"添加下载任务：hashCode =" + downloadTask.hashCode() + "  ;地址：" + downloadTask);
                    downloadQueue.add(downloadTask);
                    download();
                }
                Log.i(TAG,"任务已存在：" + downloadTask);
            }
        });
    };

    //移除下载任务
    private void removeDownloadTask(final DownloadTask downloadTask){
        waitExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if(downloadQueue.contains(downloadTask)){
                    Log.i(TAG,"删除下载任务：" + downloadTask.toString());
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
        Log.i(TAG,String.format("添加下载进程：%s",downloadTask.toString()));
        Runnable syncRun = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"准备下载：" + downloadTask.toString());
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
                        .download(downloadTask.getDownload_url())
                        .map(new Function<ResponseBody, InputStream>() {
                            @Override
                            public InputStream apply(ResponseBody responseBody) throws Exception {
                                return responseBody.byteStream();
                            }
                        })
                        .subscribe(new Consumer<InputStream>() {
                            @Override
                            public void accept(InputStream inputStream) throws Exception {
                                Log.i(TAG,"开始下载文件");
                                //开始下载文件
                                DownloadListenerImpl.getInstance().onStartDownLoad();

                                writeFile(inputStream,downloadTask.getFile_path());

                                Log.i(TAG,"下载完成");
                                //下载完成，执行下载完成回调
                                if(downloadTask.getMd5() != null && downloadTask.getMd5().length() >0){
                                    if(checkMd5(downloadTask.getFile_path(),downloadTask.getMd5())){
                                        DownloadListenerImpl.getInstance().onFinishDownload();
                                    }else{
                                        DownloadListenerImpl.getInstance().onError(new Throwable("MD5 is mismatches"));
                                    }
                                }else {
                                    DownloadListenerImpl.getInstance().onFinishDownload();
                                }
                                //移除下载任务
                                removeDownloadTask(downloadTask);

                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.e(TAG,"下载出错");
                                //下载出错
                                DownloadListenerImpl.getInstance().onError(throwable);
                                //移除下载任务，移除后DownloadListenerImpl.getInstance()中的监听对象会被置空
                                removeDownloadTask(downloadTask);

                            }
                        });
            }
        };

        downloadExecutors.execute(syncRun);
//        Future future = downloadExecutors.submit(syncRun);

    }

    //写文件
    private void writeFile(InputStream inputStream,String filePath) throws IOException {

        FileOutputStream fileOutputStream = null ;

        try {
            fileOutputStream = new FileOutputStream(filePath);

            byte[] bytes = new byte[1024];
            int length ;
            while((length = inputStream.read(bytes)) != -1 ){
                fileOutputStream.write(bytes,0,length);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            DownloadListenerImpl.getInstance().onError(e);
        }finally {
            if(fileOutputStream!= null){
                fileOutputStream.close();
            }
            if(inputStream != null){
                inputStream.close();
            }
        }

    }

    //校验md5是否相同
    private boolean checkMd5(String filePath ,String md5){
        String fileMd5 = EncryptionCheckUtil.md5sum(filePath);
        if(fileMd5 != null){
            return fileMd5.toUpperCase().equals(md5.toUpperCase());
        }
        return false ;
    }

}
