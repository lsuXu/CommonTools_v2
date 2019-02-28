package com.wxtoplink.base.download;

/**
 * Created by 12852 on 2019/2/28.
 */

enum  Status {

    DOWNLOAD_ERROR(-1),
    ORIGINAL(0),
    PREPARE(1),
    DOWNLOADING(2),
    DOWNLOAD_SUCCESS(3);

    private int id ;

    Status(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
