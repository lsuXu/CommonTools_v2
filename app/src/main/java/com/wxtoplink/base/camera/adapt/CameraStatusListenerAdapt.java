package com.wxtoplink.base.camera.adapt;

import com.wxtoplink.base.camera.interfaces.CameraStatusListener;

/**
 * Created by 12852 on 2019/1/8.
 */

public abstract class CameraStatusListenerAdapt implements CameraStatusListener{

    public final String TAG ;

    public CameraStatusListenerAdapt() {
        this.TAG = CameraStatusListenerAdapt.class.getSimpleName();
    }

    @Override
    public void onOpened() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConfigured() {

    }

    @Override
    public void onConfigureFailed() {

    }

    @Override
    public void onCaptureStarted() {

    }

    @Override
    public void onCaptureCompleted() {

    }

    @Override
    public void onCaptureFailed() {

    }

}
