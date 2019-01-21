package com.wxtoplink.base.camera.impl;

import android.content.Context;
import android.graphics.ImageFormat;

import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresPermission;
import android.util.Size;
import android.view.Surface;

import com.wxtoplink.base.camera.interfaces.CameraPreviewData;
import com.wxtoplink.base.camera.interfaces.PreviewDataCallBack;

import java.util.Arrays;
import java.util.List;


/**
 * Created by 12852 on 2018/8/29.
 */

public class NoViewPreviewCamera extends CameraTemplateImpl implements CameraPreviewData {

    private ImageReader previewImageReader ;

    private PreviewDataCallBack previewDataCallBack ;

    private HandlerThread handlerThread ;
    private Handler handler ;

    public NoViewPreviewCamera(Context context) {
        super(context);
        previewFormat = ImageFormat.YUV_420_888;
    }

    @RequiresPermission(android.Manifest.permission.CAMERA)
    @Override
    public synchronized void startPreview() {
        Size fitSize = getPreviewFitSize();
        startHandleThread();

        previewImageReader = ImageReader.newInstance(fitSize.getWidth(),fitSize.getHeight(),previewFormat,2);

        previewImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();
                if(image != null && previewDataCallBack != null){
                    previewDataCallBack.previewData(image);
                }
                image.close();
            }
        },handler);
        openCamera();
    }

    @Override
    public synchronized void stopPreview() {
        super.stopPreview();
        if(previewImageReader != null){
            previewImageReader.close();
            previewImageReader = null ;
        }

        stopHandleThread();
    }


    //预设将要输出的所有界面
    @Override
    public List<Surface> getPresetSurfaceList() {
        return Arrays.asList(previewImageReader.getSurface());
    }

    //当前需要输出数据的界面
    @Override
    public List<Surface> getPreviewSurfaceList() {
        return Arrays.asList(previewImageReader.getSurface());
    }

    @Override
    public void setPreviewDataCallBack(PreviewDataCallBack previewCallBack) {
        this.previewDataCallBack = previewCallBack ;
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
}
