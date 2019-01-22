package com.wxtoplink.base.camera.interfaces;

/**
 * Created by 12852 on 2019/1/8.
 */

public interface CameraStatusListener {

    //相机打开成功
    void onOpened();
    //相机失去连接
    void onDisconnected();
    //相机打开失败
    void onOpenError();
    //相机配置会话成功
    void onConfigured();
    //相机配置会话失败
    void onConfigureFailed();
    //相机开始进行图像捕捉
    void onCaptureStarted();
    //相机图像捕捉完成,此回调触发时若设置了预览界面，界面将会显示预览的数据
    void onCaptureCompleted();
    //相机图像捕捉失败
    void onCaptureFailed();
}
