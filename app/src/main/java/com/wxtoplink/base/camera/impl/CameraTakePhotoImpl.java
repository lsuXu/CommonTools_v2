package com.wxtoplink.base.camera.impl;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.wxtoplink.base.camera.interfaces.CameraTakePhoto;
import com.wxtoplink.base.camera.utils.CameraUtils;

import java.util.Arrays;
import java.util.List;


/**
 * Created by 12852 on 2018/8/29.
 */

public class CameraTakePhotoImpl extends CameraTemplateImpl implements CameraTakePhoto {

    private ImageReader imageReader ;
    private TextureView textureView ;

    public CameraTakePhotoImpl(Context context) {
        super(context);
        format = ImageFormat.JPEG ;
    }

    @Override
    public List<Surface> getSurfaceList() {
        return Arrays.asList(new Surface(textureView.getSurfaceTexture()));
    }

    @Override
    public List<Surface> getTotalSurfaceList() {
        return Arrays.asList(new Surface(textureView.getSurfaceTexture()),imageReader.getSurface());
    }


    @Override
    public synchronized void startPreview() {
        try {
            if(cameraId == null){
                cameraId = CameraUtils.getDefaultCameraId(context);
            }
            cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] supportSize = map.getOutputSizes(format);
            if(supportSize == null){
                throw new IllegalArgumentException(String.format(
                        "format 0x%x was not defined in either ImageFormat or PixelFormat", format));
            }
            Size fitSize = CameraUtils.getFitPreviewSize(Arrays.asList(supportSize),maxSize);
            imageReader = ImageReader.newInstance(fitSize.getWidth(),fitSize.getHeight(),format,2);

        }catch (CameraAccessException exception){
            exception.printStackTrace();
        }

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

    @Override
    public synchronized void stopPreview() {
        super.stopPreview();
        if(imageReader != null){
            imageReader.close();
            imageReader = null ;
        }
    }


    @Override
    public void setSurfaceView(TextureView view) {
        this.textureView = view ;
    }

    //拍照保存，需传入保存路径
    public void takePhoto(final String path){

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {

                Image image = imageReader.acquireNextImage();
                if(image != null){
                    CameraUtils.saveImage(image,path);
                }
                image.close();
            }
        },null);
        //执行拍照
        synchronized (CameraTakePhotoImpl.this){
            if(captureRequestBuilder != null && isPreview()){
                //设置相机进行自动对焦
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                //设置拍照后图片的旋转方向
                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, CameraUtils.getOrientation(context,cameraId));
                //将ImageReader添加到Surface中，用于接收数据
                captureRequestBuilder.addTarget(imageReader.getSurface());
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


}
