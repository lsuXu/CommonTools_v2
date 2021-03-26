package com.xuxianpu.tools.camera.impl;

import android.content.Context;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.RequiresPermission;
import android.util.Size;
import android.view.Surface;

import com.xuxianpu.tools.camera.interfaces.CameraPreviewData;
import com.xuxianpu.tools.camera.interfaces.PreviewDataCallBack;

import java.util.Arrays;
import java.util.List;


/**
 * 无界面预览使用
 * Created by 12852 on 2018/8/29.
 */

public class NoViewPreviewCamera extends CameraTemplateImpl implements CameraPreviewData {

    //预览数据获取表面
    private ImageReader previewImageReader ;

    //预览数据回调
    private PreviewDataCallBack previewDataCallBack ;

    public NoViewPreviewCamera(Context context) {
        super(context);
        previewFormat = ImageFormat.YUV_420_888;//预设YUV_420_888格式，输出帧率高
    }

    //开始预览
    @RequiresPermission(android.Manifest.permission.CAMERA)
    @Override
    public synchronized void startPreview() {
        //获取调用程序的looper
        setHostHandler();
        //打开相机
        openCamera();
    }

    //停止预览
    @Override
    public synchronized void stopPreview() {
        super.stopPreview();
        if(previewImageReader != null){
            previewImageReader.close();
            previewImageReader = null ;
        }
    }

    //初始化纹理表面
    @Override
    public void initSurface() {
        Size fitSize = getPreviewFitSize();

        previewImageReader = ImageReader.newInstance(fitSize.getWidth(),fitSize.getHeight(),previewFormat,2);

        previewImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //获取预览数据
                Image image = reader.acquireNextImage();
                if(image != null && previewDataCallBack != null){
                    //输出预览数据
                    previewDataCallBack.previewData(image);
                }
                image.close();
            }
        },getHandle());
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

    //设置预览数据回调
    @Override
    public void setPreviewDataCallBack(PreviewDataCallBack previewCallBack) {
        this.previewDataCallBack = previewCallBack ;
    }

}
