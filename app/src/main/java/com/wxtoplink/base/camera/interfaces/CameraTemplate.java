package com.wxtoplink.base.camera.interfaces;

import android.util.Size;

/**
 * Created by 12852 on 2018/8/29.
 */

public interface CameraTemplate {

    void startPreview();

    void stopPreview();

    void setCameraId(String cameraId);

    void setImageFormat(int imageFormat);

    void setMaxSize(Size maxSize);

    boolean isPreview();

}
