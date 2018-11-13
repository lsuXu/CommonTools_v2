package com.wxtoplink.base.camera;

import android.Manifest;
import android.content.Context;
import android.support.annotation.RequiresPermission;
import android.util.Size;
import android.view.TextureView;

import com.wxtoplink.base.camera.impl.CameraPreviewDataImpl;
import com.wxtoplink.base.camera.impl.CameraTakePhotoImpl;
import com.wxtoplink.base.camera.interfaces.CameraPreviewData;
import com.wxtoplink.base.camera.interfaces.CameraTakePhoto;
import com.wxtoplink.base.camera.interfaces.CameraTemplate;
import com.wxtoplink.base.camera.interfaces.PreviewDataCallBack;


/**
 * Created by 12852 on 2018/8/29.
 */

public class CameraComponent implements CameraTemplate,CameraTakePhoto,CameraPreviewData {

    public static final int CAMERA_NORMAL = 0x01 ;
    public static final int CAMERA_PREVIEW = 0x02 ;

    private boolean isInit = false ;

    private CameraTakePhotoImpl cameraTakePhoto ;

    private CameraPreviewDataImpl cameraPreviewData ;

    private Context context ;

    public static CameraComponent getInstance(){
        return CameraHolder.instance ;
    }

    //设置相机用途，使用相机组件，需要第一个被调用来初始化相机
    public void setCameraType(int cameraType,Context context){
        isInit = true ;
        if(cameraType == CameraComponent.CAMERA_NORMAL){
            if(cameraPreviewData != null){
                cameraPreviewData.stopPreview();
                cameraPreviewData = null ;
            }
            if(cameraTakePhoto == null) {
                cameraTakePhoto = new CameraTakePhotoImpl(context);
            }
        }else if(cameraType == CameraComponent.CAMERA_PREVIEW){
            if(cameraTakePhoto != null){
                cameraTakePhoto.stopPreview();
                cameraTakePhoto = null ;
            }
            if(cameraPreviewData == null){
                cameraPreviewData = new CameraPreviewDataImpl(context);
            }
        }else{
            throw new IllegalArgumentException("This cameraState " + cameraType + " is not support");
        }
    }

    //开始预览
    @Override
    @RequiresPermission(android.Manifest.permission.CAMERA)
    public synchronized void startPreview() {
        safeCheck();
        if(cameraTakePhoto != null) {
            cameraTakePhoto.startPreview();
        }
        if(cameraPreviewData != null){
            cameraPreviewData.startPreview();
        }
    }

    //停止预览
    @Override
    public synchronized void stopPreview() {
        safeCheck();
        if(cameraTakePhoto != null) {
            cameraTakePhoto.stopPreview();
            cameraTakePhoto = null ;
        }
        if(cameraPreviewData != null){
            cameraPreviewData.stopPreview();
            cameraPreviewData = null ;
        }
        isInit = false;
    }

    //摄照相机ID
    @Override
    public void setCameraId(String cameraId) {
        safeCheck();
        if(cameraTakePhoto != null) {
            cameraTakePhoto.setCameraId(cameraId);
        }
        if(cameraPreviewData != null){
            cameraPreviewData.setCameraId(cameraId);
        }
    }

    //设置图片格式
    @Override
    public void setImageFormat(int imageFormat) {
        safeCheck();
        if(cameraTakePhoto != null){
            cameraTakePhoto.setImageFormat(imageFormat);
        }
        if(cameraPreviewData != null){
            cameraPreviewData.setImageFormat(imageFormat);
        }
    }

    //设置数据最大尺寸
    @Override
    public void setMaxSize(Size maxSize) {
        safeCheck();
        if(cameraTakePhoto != null){
            cameraTakePhoto.setMaxSize(maxSize);
        }
        if(cameraPreviewData != null){
            cameraPreviewData.setMaxSize(maxSize);
        }
    }

    //设置预览界面
    @Override
    public void setSurfaceView(TextureView view) {
        safeCheck();
        if(cameraTakePhoto != null) {
            cameraTakePhoto.setSurfaceView(view);
        }
    }

    //拍照
    @Override
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void takePhoto(String filePath) {
        safeCheck();
        if(cameraTakePhoto != null) {
            cameraTakePhoto.takePhoto(filePath);
        }
    }

    //设置无界面预览数据回调
    @Override
    public void setPreviewDataCallBack(PreviewDataCallBack previewDataCallBack) {
        safeCheck();
        if(cameraPreviewData != null){
            cameraPreviewData.setPreviewDataCallBack(previewDataCallBack);
        }
    }

    private void safeCheck(){
        if(!isInit){
            throw new SecurityException("You need to call cameraType() to select the camera first");
        }
    }

    private static final class CameraHolder{
        private static final CameraComponent instance = new CameraComponent();
    }
}
