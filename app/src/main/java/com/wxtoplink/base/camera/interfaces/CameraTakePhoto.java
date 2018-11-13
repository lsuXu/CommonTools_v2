package com.wxtoplink.base.camera.interfaces;

import android.view.TextureView;

/**
 * Created by 12852 on 2018/8/29.
 */

public interface CameraTakePhoto {

    void setSurfaceView(TextureView textureView);

    void takePhoto(String filePath);
}
