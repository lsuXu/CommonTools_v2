package com.wxtoplink.base.camera.interfaces;

import android.media.Image;
import android.util.Size;

/**
 * 预览数据以及拍照数据回调
 * Created by 12852 on 2018/6/22.
 */

public interface PreviewDataCallBack {

    //实现接口获取预览数据
    void previewData(Image image);

    void errorCallBack(Exception e);

    void surfaceSizeChanged(Size size);

}
