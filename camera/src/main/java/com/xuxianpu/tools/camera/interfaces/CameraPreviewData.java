package com.xuxianpu.tools.camera.interfaces;

/**
 * 获取相机预览数据，需要获取实时预览数据的相机需要实现该接口
 * Created by 12852 on 2018/8/29.
 */

public interface CameraPreviewData {

    void setPreviewDataCallBack(PreviewDataCallBack previewCallBack);
}
