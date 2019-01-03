package com.wxtoplink.base.camera.impl;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;

import com.wxtoplink.base.camera.interfaces.CameraPreviewData;
import com.wxtoplink.base.camera.interfaces.PreviewDataCallBack;
import com.wxtoplink.base.camera.utils.CameraUtils;

import java.util.Arrays;
import java.util.List;


/**
 * Created by 12852 on 2018/8/29.
 */

public class NoViewPreviewCamera extends CameraTemplateImpl implements CameraPreviewData {

    private ImageReader imageReader ;

    private PreviewDataCallBack previewDataCallBack ;

    public NoViewPreviewCamera(Context context) {
        super(context);
        format = ImageFormat.YUV_420_888;
    }

    @Override
    public synchronized void startPreview() {

        try {
            if(cameraId == null) {
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

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();
                if(image != null && previewDataCallBack != null){
                    previewDataCallBack.previewData(image);
                }
                image.close();
            }
        },null);
        super.startPreview();
    }

    @Override
    public synchronized void stopPreview() {
        super.stopPreview();
        if(imageReader != null){
            imageReader.close();
            imageReader = null ;
        }
    }


    //预设将要输出的所有界面
    @Override
    public List<Surface> getTotalSurfaceList() {
        return Arrays.asList(imageReader.getSurface());
    }

    //当前需要输出数据的界面
    @Override
    public List<Surface> getSurfaceList() {
        return Arrays.asList(imageReader.getSurface());
    }

    @Override
    public void setPreviewDataCallBack(PreviewDataCallBack previewCallBack) {
        this.previewDataCallBack = previewCallBack ;
    }
}
