package com.wxtoplink.base.camera.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.wxtoplink.base.camera.interfaces.CameraPreviewData;
import com.wxtoplink.base.camera.interfaces.CameraTakePhoto;
import com.wxtoplink.base.camera.interfaces.CapturePhotoCallBack;
import com.wxtoplink.base.camera.interfaces.PreviewDataCallBack;
import com.wxtoplink.base.camera.utils.CameraUtils;
import com.wxtoplink.base.tools.ByteDataFormat;

import java.util.Arrays;
import java.util.List;


/**
 * Created by 12852 on 2018/8/29.
 */

public class ViewPreviewCamera extends CameraTemplateImpl implements CameraTakePhoto,CameraPreviewData{

    private ImageReader previewImageReader ;
    private ImageReader photoImageReader ;
    private TextureView textureView ;
    private PreviewDataCallBack previewDataCallBack ;

    private int photoFormat ;
    private Size photoMaxSize ;
    private Size photoFitSize ;

    public ViewPreviewCamera(Context context) {
        super(context);
        photoFormat = ImageFormat.YUV_420_888 ;//有界面以及预览数据，当前仅支持使用YUV_420_888数据格式
    }

    //初始使用的绘制表面
    @Override
    public List<Surface> getPreviewSurfaceList() {
        return Arrays.asList(new Surface(textureView.getSurfaceTexture()),
                previewImageReader.getSurface());
    }

    //所有将会用到的表面
    @Override
    public List<Surface> getPresetSurfaceList() {
        return Arrays.asList(new Surface(textureView.getSurfaceTexture()),//预览显示
                previewImageReader.getSurface(),//后台数据处理
                photoImageReader.getSurface());//拍照数据保存
    }



    @Override
    @RequiresPermission(android.Manifest.permission.CAMERA)
    public synchronized void startPreview() {

        if(textureView == null){
            throw new NullPointerException("The target canvas is null ,You should call setSurfaceView() before calling startPreview()");
        }else{
            if(textureView.isAvailable()){
                //预览界面存在，且当前状态可获取
                openCamera();
            }else{
                //预览界面没有准备好，那么设置预览界面的监听器，等它准备好后开启预览
                textureView.setSurfaceTextureListener(textureListener);
            }
        }
    }

    @Override
    public synchronized void stopPreview() {
        super.stopPreview();
        if(previewImageReader != null){
            previewImageReader.close();
            previewImageReader = null ;
        }
        if(photoImageReader != null){
            photoImageReader.close();
            photoImageReader = null ;
        }

    }

    @Override
    public void initSurface() {
        //初始化预览数据回调的imageReader
        Size fitSize = getPreviewFitSize() ;
        previewImageReader = ImageReader.newInstance(fitSize.getWidth(),fitSize.getHeight(),previewFormat,2);
        previewImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                if(previewImageReader != null) {
                    Image image = previewImageReader.acquireLatestImage();
                    if (image != null) {
                        if (previewDataCallBack != null) {
                            previewDataCallBack.previewData(image);
                        }
                        image.close();
                    }
                }
            }
        },getHandle());

        //初始化拍照回调的imageReader,使用JPEG格式，最大支持大小
        Size photoFitSize = getFitPhotoSize();
        photoImageReader = ImageReader.newInstance(photoFitSize.getWidth(),photoFitSize.getHeight(),photoFormat,1);
    }


    @Override
    public void setSurfaceView(TextureView view) {
        this.textureView = view ;
    }

    @Override
    public void setPhotoFormat(int photoFormat) {
        this.photoFormat = photoFormat ;
    }

    @Override
    public void setPhotoMaxSize(Size photoMaxSize) {
        this.photoMaxSize = photoMaxSize ;
    }

    @Override
    public Size getFitPhotoSize() {
        if(photoFitSize == null){
            photoFitSize = CameraUtils.getFitSize(context,getCameraId(),photoFormat,photoMaxSize);
        }
        return photoFitSize;
    }

    //拍照保存，需传入保存路径
    public void takePhoto(final String filePath){
        takePhoto(filePath,false);
    }

    @Override
    public void takePhoto(String filePath, boolean stopPreview) {
        initPhotoImageReader(filePath);
        capturePhoto(stopPreview);
    }

    @Override
    public void takePhoto(@NonNull CapturePhotoCallBack capturePhotoCallBack) {
        takePhoto(capturePhotoCallBack,false);
    }

    @Override
    public void takePhoto(CapturePhotoCallBack capturePhotoCallBack, boolean stopPreview) {
        initPhotoImageReader(capturePhotoCallBack);
        capturePhoto(stopPreview);
    }

    //建立拍照请求
    private void capturePhoto(boolean stopPreview){
        //执行拍照
        synchronized (ViewPreviewCamera.this){
            if(captureRequestBuilder != null && isPreview()){
                //设置相机进行自动对焦
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                //设置拍照后图片的旋转方向
                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, CameraUtils.getOrientation(context,getCameraId()));
                //将ImageReader添加到Surface中，用于接收数据
                captureRequestBuilder.addTarget(photoImageReader.getSurface());
                if(cameraCaptureSession != null) {//相机一旦创建新的会话，原先的会话就会被关闭置空，加入判断
                    try {
                        if (stopPreview) {//若需要在拍照后停止预览，则在发送拍照请求前应先停止预览，在拍照回调中停止预览会造成死锁问题
                            cameraCaptureSession.stopRepeating();
                        }
                        cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, getHandle());
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    //textureView的状态回调处理
    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener(){

        @SuppressLint("MissingPermission")
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            startPreview();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            //若视图被销毁，停止预览
            stopPreview();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    //初始化拍照ImageReader
    private void initPhotoImageReader(final String path){

        photoImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if(image != null){
                    if(image.getFormat() == ImageFormat.JPEG) {
                        CameraUtils.saveImage(image, path);
                    }else if(image.getFormat() == ImageFormat.YUV_420_888){
                        byte [] nv21Byte = ByteDataFormat.formatYUV420_888ToNV21(image);
                        byte [] jpegByte = ByteDataFormat.NV21_2_JPEG(nv21Byte,image.getWidth(),image.getHeight());
                        CameraUtils.saveImage(jpegByte,path);
                    }else{//直接保存照片的拍照方式仅支持JPEG格式以及YUV_420_888格式，其余格式，调用initTakePhotoImageReader(CapturePhotoCallBack)自行实现
                        image.close();
                        throw new IllegalArgumentException("The takePhoto('String') method supports olay JPEG and YUV_420_888 format " +
                                ",recommended to call takePhoto(CapturePhotoCallBack)");
                    }
                    image.close();
                }
            }
        },getHandle());
    }

    //初始化ImagerReader
    private void initPhotoImageReader(@NonNull final CapturePhotoCallBack capturePhotoCallBack){
        photoImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if(image != null){
                    capturePhotoCallBack.captureData(image);
                    image.close();
                }
            }
        },getHandle());
    }

    @Override
    public void setPreviewDataCallBack(PreviewDataCallBack previewCallBack) {
        this.previewDataCallBack = previewCallBack ;
    }
}
