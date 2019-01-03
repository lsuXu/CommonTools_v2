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
import android.os.Looper;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.wxtoplink.base.camera.interfaces.CameraPreviewData;
import com.wxtoplink.base.camera.interfaces.CameraTakePhoto;
import com.wxtoplink.base.camera.interfaces.PreviewDataCallBack;
import com.wxtoplink.base.camera.utils.CameraUtils;

import java.util.Arrays;
import java.util.List;


/**
 * Created by 12852 on 2018/8/29.
 */

public class ViewPreviewCamera extends CameraTemplateImpl implements CameraTakePhoto,CameraPreviewData{

    private ImageReader imageReaderPreview ;
    private ImageReader imageReaderTakePhoto ;
    private TextureView textureView ;
    private HandlerThread handlerThread ;
    private Handler handler ;
    private PreviewDataCallBack previewDataCallBack ;

    public ViewPreviewCamera(Context context) {
        super(context);
        format = ImageFormat.YUV_420_888 ;
        startHandleThread();
    }

    //初始使用的绘制表面
    @Override
    public List<Surface> getSurfaceList() {
        return Arrays.asList(new Surface(textureView.getSurfaceTexture()),
                imageReaderPreview.getSurface());
    }

    //所有将会用到的表面
    @Override
    public List<Surface> getTotalSurfaceList() {
        return Arrays.asList(new Surface(textureView.getSurfaceTexture()),//预览显示
                imageReaderPreview.getSurface(),//后台数据处理
                imageReaderTakePhoto.getSurface());//拍照数据保存
    }

    private void initImagerReader(){
        //初始化预览数据回调的imageReader
        Size fitSize =  CameraUtils.getFitPreviewSize(context,cameraId,format,maxSize) ;
        imageReaderPreview = ImageReader.newInstance(fitSize.getWidth(),fitSize.getHeight(),format,2);
        imageReaderPreview.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = imageReaderPreview.acquireLatestImage();
                if(image != null){
                    if(previewDataCallBack != null){
                        previewDataCallBack.previewData(image);
                    }
                    image.close();
                }
            }
        },handler);

        //初始化拍照回调的imageReader,使用JPEG格式，最大支持大小
        Size size = CameraUtils.getFitPreviewSize(context,cameraId,ImageFormat.JPEG,null);
        imageReaderTakePhoto = ImageReader.newInstance(size.getWidth(),size.getHeight(),ImageFormat.JPEG,1);
    }


    @SuppressLint("MissingPermission")
    @Override
    public synchronized void startPreview() {

        //初始化imagerReader
        initImagerReader();

        if(textureView == null){
            throw new NullPointerException("The target canvas is null ,You should call setSurfaceView() before calling startPreview()");
        }else{
            if(textureView.isAvailable()){
                //预览界面存在，且当前状态可获取
                super.startPreview();
            }else{
                //预览界面没有准备好，那么设置预览界面的监听器，等它准备好后再重新开始预览
                textureView.setSurfaceTextureListener(textureListener);
            }
        }
    }

    private void startHandleThread(){

        handlerThread = new HandlerThread("imageReaderHandle");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    private void stopHandleThread() {
        if(handlerThread != null) {
            handlerThread.quitSafely();
            try {
                handlerThread.join();
                handlerThread = null;
                handler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void stopPreview() {
        super.stopPreview();
        if(imageReaderPreview != null){
            imageReaderPreview.close();
            imageReaderPreview = null ;
        }
        if(imageReaderTakePhoto != null){
            imageReaderTakePhoto.close();
            imageReaderTakePhoto = null ;
        }

        stopHandleThread();
    }


    @Override
    public void setSurfaceView(TextureView view) {
        this.textureView = view ;
    }

    //拍照保存，需传入保存路径
    public void takePhoto(final String path){

        initTakePhotoImageReader(path);

        //执行拍照
        synchronized (ViewPreviewCamera.this){
            if(captureRequestBuilder != null && isPreview()){
                //设置相机进行自动对焦
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                //设置拍照后图片的旋转方向
                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, CameraUtils.getOrientation(context,cameraId));
                //将ImageReader添加到Surface中，用于接收数据
                captureRequestBuilder.addTarget(imageReaderTakePhoto.getSurface());
                try {
                    cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    //textureView的状态回调处理
    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener(){

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
    private void initTakePhotoImageReader(final String path){

        imageReaderTakePhoto.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if(image != null){
                    CameraUtils.saveImage(image,path);
                    image.close();
                }
            }
        },handler);
    }

    @Override
    public void setPreviewDataCallBack(PreviewDataCallBack previewCallBack) {
        this.previewDataCallBack = previewCallBack ;
    }
}
