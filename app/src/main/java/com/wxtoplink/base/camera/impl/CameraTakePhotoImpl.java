package com.wxtoplink.base.camera.impl;

import android.annotation.SuppressLint;
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
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.wxtoplink.base.camera.interfaces.CameraTakePhoto;
import com.wxtoplink.base.camera.interfaces.CapturePhotoCallBack;
import com.wxtoplink.base.camera.utils.CameraUtils;
import com.wxtoplink.base.tools.ByteDataFormat;

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
        format = ImageFormat.JPEG ;//无界面预览模式，故默认使用JPEG格式进行拍照，还支持YUV_420_888数据格式
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
        initTakePhotoImageReader(path);
        capturePhoto();
    }

    @Override
    public void takePhoto(CapturePhotoCallBack capturePhotoCallBack) {
        initTakePhotoImageReader(capturePhotoCallBack);
        capturePhoto();
    }

    //建立拍照请求
    public void capturePhoto(){
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


    //初始化拍照ImageReader
    private void initTakePhotoImageReader(final String path){

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
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
                }
            }
        },null);
    }

    //初始化ImagerReader
    private void initTakePhotoImageReader(@NonNull final CapturePhotoCallBack capturePhotoCallBack){
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if(image != null){
                    capturePhotoCallBack.captureData(image);
                    image.close();
                }
            }
        },null);
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


}
