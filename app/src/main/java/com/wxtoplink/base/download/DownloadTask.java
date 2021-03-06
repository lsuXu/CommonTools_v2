package com.wxtoplink.base.download;

import com.wxtoplink.base.download.listener.DownloadListener;

/**
 * Created by 12852 on 2018/7/26.
 */

public class DownloadTask {

    //下载路径
    private String download_url ;
    //下载的文件名
    private String file_name ;
    //文件的保存路径，全路径
    private String file_path ;
    //hash值,选填
    private String md5 ;
    //下载状态(0:排队，1:下载队列中，2:正在下载:3)
    private int status ;
    //下载文件的监听器
    private DownloadListener downloadListener ;
    //标记对象
    private Object tag ;


    public DownloadTask(String download_url, String file_name, String file_path, String md5, DownloadListener downloadListener) {
        this.download_url = download_url;
        this.file_name = file_name;
        this.file_path = file_path;
        this.md5 = md5;
        this.downloadListener = downloadListener;
        this.status = Status.ORIGINAL.getId();
    }

    public String getDownload_url() {
        return download_url;
    }

    public void setDownload_url(String download_url) {
        this.download_url = download_url;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status.getId();
    }

    public DownloadListener getDownloadListener() {
        return downloadListener;
    }

    public void setDownloadListener(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return "DownloadTask{" +
                "download_url='" + download_url + '\'' +
                ", file_name='" + file_name + '\'' +
                ", file_path='" + file_path + '\'' +
                ", md5='" + md5 + '\'' +
                ", status=" + status +
                ", downloadListener=" + downloadListener +
                '}';
    }
}
