package com.wxtoplink.base.camera.interfaces;

import android.util.Size;

/**
 * camera模板
 * Created by 12852 on 2018/8/29.
 */

public interface CameraTemplate {

    //开始预览
    void startPreview();

    //打开下爱你
    void openCamera();

    //停止预览
    void stopPreview();

    //设置相机方向
    void setCameraId(String cameraId);

    //设置预览格式
    void setPreviewFormat(int previewFormat);

    //设置预览允许的最大大小
    void setPreviewMaxSize(Size maxSize);

    //获取预览的最适宜尺寸
    Size getPreviewFitSize();

    //判断是否正在预览
    boolean isPreview();

    //初始化纹理表面
    void initSurface();

    //设置相机状态监听器
    void setCameraStatusListener(CameraStatusListener cameraStatusListener);

}
