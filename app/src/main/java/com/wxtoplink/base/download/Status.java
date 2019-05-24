package com.wxtoplink.base.download;

/**
 * 下载状态
 * Created by 12852 on 2019/2/28.
 */

enum  Status {

    DOWNLOAD_ERROR(-1),//下载失败
    ORIGINAL(0),//初始状态
    PREPARE(1),//准备状态
    DOWNLOADING(2),//下载中
    DOWNLOAD_SUCCESS(3);//下载成功

    private int id ;

    Status(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
