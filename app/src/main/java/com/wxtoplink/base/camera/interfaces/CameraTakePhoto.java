package com.wxtoplink.base.camera.interfaces;

import android.util.Size;
import android.view.TextureView;

/**
 * Created by 12852 on 2018/8/29.
 */

public interface CameraTakePhoto {

    void setSurfaceView(TextureView textureView);//设置预览视图

    void setPhotoFormat(int photoFormat);//设置照片格式

    void setPhotoMaxSize(Size photoMaxSize);//设置照片最大大小

    Size getFitPhotoSize();//获取适合的照片大小，根据photoMaxSize计算得出

    void takePhoto(String filePath);//拍照直接保存到文件

    void takePhoto(String filePath , boolean stopPreview);//拍照直接保存到文件，可选拍照后是否直接停止预览

    void takePhoto(CapturePhotoCallBack capturePhotoCallBack);//拍照

    void takePhoto(CapturePhotoCallBack capturePhotoCallBack,boolean stopPreview);//拍照回调数据，可选拍照后是否直接停止预览

}
