package com.wxtoplink.base.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;

import com.wxtoplink.base.log.AbstractLog;
import com.wxtoplink.base.log.CameraLog;


/**
 * CameraDevice持有者，单例引用,仅保持成功的CameraDevice的引用
 * Created by 12852 on 2019/1/30.
 */

public final class Camera2Holder {

    private static final String TAG = Camera2Holder.class.getSimpleName();

    private CameraManager cameraManager;//camera管理器

    private static CameraDevice cameraDevice = null;//camera实例，单例存在

    private CameraDevice.StateCallback outStateCallback ;//外部引用CameraDevice回调

    private HandlerThread cameraHandleThread ;

    private Handler cameraHandler ;

    private AbstractLog logInstance ;

    private Camera2Holder() {
        logInstance = CameraLog.getInstance();
    }

    public static Camera2Holder getInstance() {
        return Holder.instance;
    }

    public CameraManager getCameraManager(Context context) {
        if (cameraManager == null) {
            cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        }
        return cameraManager;
    }

    private Handler getHandle(){
        if(cameraHandler == null){
            startCameraThread();
        }
        return cameraHandler;
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(android.Manifest.permission.CAMERA)
    public void preOpenCamera(@NonNull Context context,@NonNull String cameraId) throws CameraAccessException{
        logInstance.i(TAG,"preOpenCamera(), init cameraDevice");
        closeCameraDevice();//清除已经打开的Camera实例
        getCameraManager(context).openCamera(cameraId, inStateCallback, getHandle());
    }

    //打开相机，优先使用缓存，提高camera转换效率
    @SuppressLint("MissingPermission")
    public void openCamera(@NonNull Context context, @NonNull String cameraId, CameraDevice.StateCallback stateCallback) throws CameraAccessException {
        outStateCallback = stateCallback ;
        //若当前存在已经打开的相机实例，且方向一致，则直接返回实例
        if(cameraDevice != null && cameraId.equals(cameraDevice.getId())){
            logInstance.i(TAG,"CameraDevice is already init,return " + cameraDevice);
            stateCallback.onOpened(cameraDevice);
        }else{
            preOpenCamera(context,cameraId);
        }
    }

    //camera实例是否打开
    public boolean isOpened(){
        return cameraDevice != null;
    }

    public void release(){
        closeCameraDevice();//关闭相机
        stopCameraThread();//停止线层
    }

    //关闭已打开的camera实例
    private void closeCameraDevice(){
        if(cameraDevice != null){
            logInstance.i(TAG,"closeCameraDevice()");
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    /**
     * 启动一个后台线程以及它的Handle
     * Starts a background thread and its {@link Handler}.
     */
    private void startCameraThread() {
        cameraHandleThread = new HandlerThread("CameraDeviceThread");
        cameraHandleThread.start();
        cameraHandler = new Handler(cameraHandleThread.getLooper());
    }

    /**
     * 停止后台线程以及它的Handle
     * Stops the background thread and its {@link Handler}.
     */
    private void stopCameraThread() {
        if(cameraHandleThread != null) {
            cameraHandleThread.quitSafely();
            try {
                cameraHandleThread.join();
                cameraHandleThread = null;
                cameraHandler = null;
            } catch (InterruptedException e) {
                logInstance.e(TAG,"stopCameraThread() error",e);
                e.printStackTrace();
            }
        }
    }

    private static final class Holder{
        private static final Camera2Holder instance = new Camera2Holder();
    }

    //内部使用回调
    private final CameraDevice.StateCallback inStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            logInstance.i(TAG,"CameraDevice onOpened");
            //相机打开成功
            cameraDevice = camera ;

            if(outStateCallback != null){
                outStateCallback.onOpened(cameraDevice);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            logInstance.e(TAG,"CameraDevice onDisconnected()");
            camera.close();
            cameraDevice = null ;
            if(outStateCallback != null){
                outStateCallback.onDisconnected(camera);
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            logInstance.e(TAG,"CameraDevice onError():error status =" + error);
            closeCameraDevice();//发送错误，关闭CameraDevice
            if(outStateCallback != null){
                outStateCallback.onError(camera,error);
            }else{
                logInstance.e(TAG,"outStateCallback = null");
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            logInstance.e(TAG,"CameraDevice onClosed()");
            super.onClosed(camera);
        }
    };

}
