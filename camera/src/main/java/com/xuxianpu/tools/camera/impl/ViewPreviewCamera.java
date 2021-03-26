package com.xuxianpu.tools.camera.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.xuxianpu.tools.camera.interfaces.CameraPreviewData;
import com.xuxianpu.tools.camera.interfaces.CameraTakePhoto;
import com.xuxianpu.tools.camera.interfaces.CapturePhotoCallBack;
import com.xuxianpu.tools.camera.interfaces.PreviewDataCallBack;
import com.xuxianpu.tools.camera.utils.ByteDataFormat;
import com.xuxianpu.tools.camera.utils.CameraUtils;

import java.util.Arrays;
import java.util.List;


/**
 * 带预览界面的数据预览使用
 * Created by 12852 on 2018/8/29.
 */

public class ViewPreviewCamera extends CameraTemplateImpl implements CameraTakePhoto, CameraPreviewData {

    private ImageReader previewImageReader ;//预览数据获取源
    private ImageReader photoImageReader ;//拍照数据获取源
    private TextureView textureView ;//预览显示视图
    private PreviewDataCallBack previewDataCallBack ;//预览数据回调

    private int photoFormat ;//照片格式
    private Size photoMaxSize ;//照片最大尺寸
    private Size photoFitSize ;//照片最适宜的尺寸

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


    //开始预览
    @Override
    @RequiresPermission(android.Manifest.permission.CAMERA)
    public synchronized void startPreview() {
        setHostHandler();

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

    //停止预览
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

    //初始化纹理表面
    @Override
    public void initSurface() {
        //初始化预览数据回调的imageReader
        Size fitSize = getPreviewFitSize() ;
        previewImageReader = ImageReader.newInstance(fitSize.getWidth(),fitSize.getHeight(),previewFormat,2);
        previewImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                if(previewImageReader != null) {
                    //获取预览数据
                    Image image = previewImageReader.acquireLatestImage();
                    if (image != null) {
                        if (previewDataCallBack != null) {
                            //输出预览数据
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


    //设置预览视图
    @Override
    public void setSurfaceView(TextureView view) {
        this.textureView = view ;
    }

    //设置拍照格式
    @Override
    public void setPhotoFormat(int photoFormat) {
        this.photoFormat = photoFormat ;
    }

    //设置拍照照片最大大小
    @Override
    public void setPhotoMaxSize(Size photoMaxSize) {
        this.photoMaxSize = photoMaxSize ;
    }

    //获取拍照最适宜的尺寸
    @Override
    public Size getFitPhotoSize() {
        if(photoFitSize == null){
            //根据相机方向，图片格式以及允许的最大大小，获取最合适的照片大小
            photoFitSize = CameraUtils.getFitSize(context,getCameraId(),photoFormat,photoMaxSize);
        }
        return photoFitSize;
    }

    /**
     * 拍照保存到指定路径
     * @param filePath 文件路径
     */
    public void takePhoto(final String filePath){
        takePhoto(filePath,false);
    }

    /**
     * 拍照保存到指定文件路径
     * @param filePath 文件路径
     * @param stopPreview 拍照的后续操作，true为停止预览
     */
    @Override
    public void takePhoto(String filePath, boolean stopPreview) {
        initPhotoImageReader(filePath);
        capturePhoto(stopPreview);
    }

    /**
     * 拍照回调，直接获取拍照的源数据进行处理
     * @param capturePhotoCallBack 拍照回调
     */
    @Override
    public void takePhoto(@NonNull CapturePhotoCallBack capturePhotoCallBack) {
        takePhoto(capturePhotoCallBack,false);
    }

    /**
     * 拍照回调，直接获取拍照的源数据进行处理，可选是否在拍照后停止预览
     * @param capturePhotoCallBack 拍照回调
     * @param stopPreview true停止预览
     */
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
                    if(image.getFormat() == ImageFormat.JPEG) {//图片格式为JPEG
                        //保存图片
                        CameraUtils.saveImage(image, path);
                    }else if(image.getFormat() == ImageFormat.YUV_420_888){//图片格式为YUV_420_888，进行转码后保存
                        //数据格式转换（YUV_420_888->Nv21）
                        byte [] nv21Byte = ByteDataFormat.formatYUV420_888ToNV21(image);
                        //数据格式转换（Nv21->JPEG）
                        byte [] jpegByte = ByteDataFormat.NV21_2_JPEG(nv21Byte,image.getWidth(),image.getHeight());
                        //保存图片
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
                //获取照片数据
                Image image = reader.acquireLatestImage();
                if(image != null){
                    //发送照片数据
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
