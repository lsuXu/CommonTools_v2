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
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
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
 * 相机模板对于拍照功能的实现，提供画面预览以及拍照功能
 * Created by 12852 on 2018/8/29.
 */

public class CameraTakePhotoImpl extends CameraTemplateImpl implements CameraTakePhoto {

    private ImageReader photoImageReader ;//照片数据获取
    private TextureView textureView ;//预览界面
    private int photoFormat ;//照片格式
    private Size photoMaxSize ;//照片最大格式
    private Size photoFitSize ;//照片合适的大小

    public CameraTakePhotoImpl(Context context) {
        super(context);
        photoFormat = ImageFormat.JPEG ;//无界面预览模式，默认使用JPEG格式进行拍照，还支持YUV_420_888数据格式
    }

    /**
     * 相机初次打开时的默认预览输出表面
     * @return  初次打开默认的预览输出表面，必须为{@link CameraTakePhotoImpl#getPresetSurfaceList()}输出的子集
     */
    @Override
    public List<Surface> getPreviewSurfaceList() {
        return Arrays.asList(new Surface(textureView.getSurfaceTexture()));
    }

    /**
     * 预设所有的输出表面，只有预设的表面，才可以动态的添加到真正的输出表面
     * @return  预设的所有输出表面列表
     */
    @Override
    public List<Surface> getPresetSurfaceList() {
        return Arrays.asList(new Surface(textureView.getSurfaceTexture()),photoImageReader.getSurface());
    }

    /**
     * 开始预览，获取当前的looper,用于回调状态执行，等待预览的纹理表面可用后，打开相机，进行配置
     */
    @RequiresPermission(android.Manifest.permission.CAMERA)
    @Override
    public synchronized void startPreview() {

        //获取调用程序的looper
        setHostHandler();

        if(textureView == null){
            throw new NullPointerException("The target canvas is null ,You should call setSurfaceView() before calling startPreview()");
        }else{
            if(textureView.isAvailable()){//预览界面存在，且当前状态可用于呈现
                //打开相机
                openCamera();
            }else{
                //预览界面没有准备好，那么设置预览界面的监听器，等它准备好后再重新开始预览
                textureView.setSurfaceTextureListener(textureListener);
            }
        }
    }

    //停止预览
    @Override
    public synchronized void stopPreview() {
        super.stopPreview();
        if(photoImageReader != null){
            photoImageReader.close();
            photoImageReader = null ;
        }
        if(textureView != null){
            textureView = null ;
        }
    }

    //初始化纹理表面
    @Override
    public void initSurface() {
        Size fitSize = getFitPhotoSize();
        photoImageReader = ImageReader.newInstance(fitSize.getWidth(),fitSize.getHeight(),photoFormat,1);
    }

    //设置预览视图
    @Override
    public void setSurfaceView(TextureView view) {
        this.textureView = view ;
    }

    //设置照片格式
    @Override
    public void setPhotoFormat(int photoFormat) {
        this.photoFormat = photoFormat ;
    }

    //设置照片最大尺寸
    @Override
    public void setPhotoMaxSize(Size photoMaxSize) {
        this.photoMaxSize = photoMaxSize ;
    }

    //获取最合适的照片尺寸大小
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
        initTakePhotoImageReader(filePath);
        capturePhoto(stopPreview);
    }

    /**
     * 拍照回调，直接获取拍照的源数据进行处理
     * @param capturePhotoCallBack 拍照回调
     */
    @Override
    public void takePhoto(CapturePhotoCallBack capturePhotoCallBack) {
        takePhoto(capturePhotoCallBack,false);
    }

    /**
     * 拍照回调，直接获取拍照的源数据进行处理，可选是否在拍照后停止预览
     * @param capturePhotoCallBack 拍照回调
     * @param stopPreview true停止预览
     */
    @Override
    public void takePhoto(CapturePhotoCallBack capturePhotoCallBack, boolean stopPreview) {
        initTakePhotoImageReader(capturePhotoCallBack);
        capturePhoto(stopPreview);
    }

    //建立拍照请求
    public void capturePhoto(boolean stopPreview){
        //执行拍照
        synchronized (CameraTakePhotoImpl.this){
            if(captureRequestBuilder != null && isPreview()){
                //设置相机进行自动对焦
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                //设置拍照后图片的旋转方向
                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, CameraUtils.getOrientation(context,getCameraId()));
                //将ImageReader添加到Surface中，用于接收数据
                captureRequestBuilder.addTarget(photoImageReader.getSurface());
                try {
                    if(stopPreview){//若需要在拍照后停止预览，则在发送拍照请求前应先停止预览，在拍照回调中停止预览会造成死锁问题
                        cameraCaptureSession.stopRepeating();
                    }
                    cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    //初始化拍照ImageReader
    private void initTakePhotoImageReader(final String filePath){

        photoImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if(image != null){
                    if(image.getFormat() == ImageFormat.JPEG) {
                        CameraUtils.saveImage(image, filePath);
                        image.close();
                    }else if(image.getFormat() == ImageFormat.YUV_420_888){
                        byte [] nv21Byte = ByteDataFormat.formatYUV420_888ToNV21(image);
                        byte [] jpegByte = ByteDataFormat.NV21_2_JPEG(nv21Byte,image.getWidth(),image.getHeight());
                        CameraUtils.saveImage(jpegByte,filePath);
                        image.close();
                    }else{//直接保存照片的拍照方式仅支持JPEG格式以及YUV_420_888格式，其余格式，调用initTakePhotoImageReader(CapturePhotoCallBack)自行实现
                        image.close();
                        throw new IllegalArgumentException("The takePhoto('String') method supports olay JPEG and YUV_420_888 format " +
                                ",recommended to call takePhoto(CapturePhotoCallBack)");
                    }
                }
            }
        },getHandle());
    }

    //初始化ImagerReader
    private void initTakePhotoImageReader(@NonNull final CapturePhotoCallBack capturePhotoCallBack){
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


    //textureView的状态回调处理
    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener(){

        @SuppressLint("MissingPermission")
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            startPreview();
            textureView.setSurfaceTextureListener(null);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            //若视图被销毁，停止预览
            stopPreview();
            textureView.setSurfaceTextureListener(null);
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


}
