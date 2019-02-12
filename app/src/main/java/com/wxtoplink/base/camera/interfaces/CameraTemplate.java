package com.wxtoplink.base.camera.interfaces;

import android.util.Size;

/**
 * Created by 12852 on 2018/8/29.
 */

public interface CameraTemplate {

    void startPreview();

    void openCamera();

    void stopPreview();

    void setCameraId(String cameraId);

    void setPreviewFormat(int previewFormat);

    void setPreviewMaxSize(Size maxSize);

    Size getPreviewFitSize();

    boolean isPreview();

    void initSurface();

    void setCameraStatusListener(CameraStatusListener cameraStatusListener);

}
