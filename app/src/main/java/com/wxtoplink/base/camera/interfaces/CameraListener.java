package com.wxtoplink.base.camera.interfaces;

import java.util.List;

/**
 * Created by 12852 on 2019/1/8.
 */

public interface CameraListener {

    boolean addListener(CameraStatusListener cameraStatusListener);

    boolean removeListener(CameraStatusListener cameraStatusListener);

    Iterable<CameraStatusListener> getListenerQueue();

    void removeAllListener();
}
