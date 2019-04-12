package com.wxtoplink.base.download;

import com.wxtoplink.base.download.adapt.DownloadListenerAdapt;
import com.wxtoplink.base.download.listener.DownloadInterceptor;
import com.wxtoplink.base.download.listener.DownloadListener;
import com.wxtoplink.base.download.utils.DownloadUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import retrofit2.adapter.rxjava2.HttpException;

/**
 * 下载线层
 * Created by 12852 on 2019/3/1.
 */

public final class DownloadService implements Runnable{

    private final DownloadTask downloadTask ;

    private final DownloadListener downloadListener ;

    private final Observer observer ;

    private boolean uniqueIdentifierSupport ;//存在唯一ID支持

    private String breakFilePath ;

    private long startRange , totalSize ;//开始范围，以及文件总大小，文件总大小为-1表示获取文件总大小失败

    private Observable<ResponseBody> responseBodyObservable ;

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

        setDownloadStatus(Status.PREPARE);//准备下载

        totalSize = getSize(downloadTask.getDownload_url());//文件总大小

        startRange = DownloadUtil.getRangeStart(downloadTask);//开始范围

        uniqueIdentifierSupport = DownloadUtil.breakSupport(downloadTask);

        if(uniqueIdentifierSupport){//唯一标志支持
            breakFilePath = DownloadUtil.generateBreakFilePath(downloadTask);
        }

        //满足断点续传的所有条件，进行端点续传
        if(uniqueIdentifierSupport){
            if(totalSize == -1){//说明文件长度获取失败，直接使用断点续传
                initBreakPointDownload();
            }else if(totalSize != 0 && startRange < totalSize) {
                initBreakPointDownload();
            }else if(totalSize != 0){//文件大小不为0，但是当前大小已经超出了文件范围
                File file = new File(breakFilePath);
                if(DownloadUtil.checkMd5(breakFilePath,downloadTask.getMd5())){//MD5匹配成功，重命名，触发完成回调
                    file.renameTo(new File(downloadTask.getFile_path()));
                    setDownloadStatus(Status.DOWNLOAD_SUCCESS);
                    observer.downloadFinish(this);
                    return;
                }else{
                    file.delete();
                    startRange = DownloadUtil.getRangeStart(downloadTask);//重置开始范围
                    initBreakPointDownload();
                }
            }else{
                initNormalDownload();
            }
        }else {//不支持断点续传，直接下载
            initNormalDownload();
        }


        responseBodyObservable
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
                        boolean success = writeFile(inputStream, downloadTask);

                        if (success) {
                            //下载完成，执行下载完成回调
                            if (downloadTask.getMd5() != null && downloadTask.getMd5().length() > 0) {
                                if (DownloadUtil.checkMd5(downloadTask.getFile_path(), downloadTask.getMd5())) {
                                    setDownloadStatus(Status.DOWNLOAD_SUCCESS);//下载成功
                                } else {
                                    setDownloadStatus(Status.DOWNLOAD_ERROR, new Throwable("MD5 is mismatches"));//下载失败
                                }
                            } else {
                                setDownloadStatus(Status.DOWNLOAD_SUCCESS);//下载成功
                            }
                        } else {
                            setDownloadStatus(Status.DOWNLOAD_ERROR, new Throwable("Download fail"));//下载失败
                        }
                        //移除下载任务
                        observer.downloadFinish(DownloadService.this);

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                        //若为预期的错误，且文件矫正通过，则触发成功回调，否则删除文件后触发失败回调
                        if (expectedErrorDeal(throwable)) {
                            setDownloadStatus(Status.DOWNLOAD_SUCCESS);//下载成功
                        } else {
                            //下载出错
                            setDownloadStatus(Status.DOWNLOAD_ERROR, throwable);//下载失败
                        }
                        observer.downloadFinish(DownloadService.this);
                    }
                });
    }

    private void initBreakPointDownload(){
        responseBodyObservable = RetrofitHelper.getInstance()
                .getRetrofit(new DownloadInterceptor(downloadListener, startRange))
                .create(IDownload.class)
                .download(String.format("bytes=%s-%s", startRange, totalSize == -1?"":totalSize), downloadTask.getDownload_url());
    }

    private void initNormalDownload(){
        responseBodyObservable = RetrofitHelper.getInstance()
                .getRetrofit(new DownloadInterceptor(downloadListener, startRange))
                .create(IDownload.class)
                .download(downloadTask.getDownload_url());
    }


    //根据条件写入文件
    private boolean writeFile(InputStream inputStream , DownloadTask downloadTask){
        if(uniqueIdentifierSupport){//支持断点重传，获取重传文件名
            boolean success = DownloadUtil.writeFile(inputStream,breakFilePath,startRange);
            if(success){
                File file = new File(breakFilePath);
                return file.renameTo(new File(downloadTask.getFile_path()));
            }
            return false ;
        }else{//不支持文件重传
            return DownloadUtil.writeFile(inputStream,downloadTask.getFile_path());
        }
    }

    //416错误，断点续传部分，range超出文件总大小范围
    private boolean expectedErrorDeal(Throwable throwable){
        if((throwable instanceof HttpException && ((HttpException) throwable).code() == 416)
                ||(throwable instanceof retrofit2.HttpException && ((retrofit2.HttpException) throwable).code() == 416)){
            if(DownloadUtil.checkMd5(breakFilePath,downloadTask.getMd5())){
                File file = new File(breakFilePath);
                return file.renameTo(new File(downloadTask.getFile_path()));
            }else {
                File breakFile = new File(breakFilePath);
                if(breakFile.exists()){
                    breakFile.delete() ;
                }
            }
        }

        return false ;
    }

    //获取网络资源文件大小
    private long getSize(String urlpath) {
        try {
            URL u = new URL(urlpath);
            HttpURLConnection urlcon = (HttpURLConnection) u.openConnection();
            urlcon.addRequestProperty("Accept-Encoding", "identity");
            return urlcon.getContentLength();
        }catch (IOException io){
            io.printStackTrace();
        }
        return -1;
    }

    private void setDownloadStatus(Status status){
        setDownloadStatus(status,null);
    }

    //设置下载状态
    private void setDownloadStatus(Status status ,Throwable throwable){
        switch (status){
            case ORIGINAL:
                downloadTask.setStatus(Status.ORIGINAL);
                break;
            case PREPARE:
                downloadTask.setStatus(Status.PREPARE);
                downloadListener.onPrepareDownload(downloadTask);
                break;
            case DOWNLOADING:
                downloadTask.setStatus(Status.DOWNLOADING);
                break;
            case DOWNLOAD_SUCCESS:
                downloadTask.setStatus(Status.DOWNLOAD_SUCCESS);
                downloadListener.onFinishDownload();
                break;
            case DOWNLOAD_ERROR:
                downloadTask.setStatus(Status.DOWNLOAD_ERROR);
                downloadListener.onError(throwable);
                break;
                default:break;
        }
    }

}
