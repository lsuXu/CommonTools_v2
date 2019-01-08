package com.wxtoplink.base.camera.interfaces;

import android.media.Image;

/**
 * Created by 12852 on 2019/1/4.
 */

public interface CapturePhotoCallBack {
    //实现接口获取预览数据
    void captureData(Image image);
}
