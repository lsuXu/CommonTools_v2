package com.wxtoplink.base.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.util.Log;


/**
 * CameraDevice持有者，单例引用,仅保持成功的CameraDevice的引用
 * Created by 12852 on 2019/1/30.
 */

public final class Camera2Holder {

    private static final String TAG = Camera2Holder.class.getSimpleName();

    private CameraManager cameraManager;//camera管理器

    private CameraDevice cameraDevice;//camera实例，单例存在

    private CameraCaptureSession captureSession ;//captureSession实例，单例存在

    private CameraDevice.StateCallback outStateCallback ;//外部引用CameraDevice回调

    private HandlerThread cameraHandleThread ;

    private Handler cameraHandler ;

    private String cameraId ;

    private Camera2Holder() {
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
        closeCameraDevice();//清除已经打开的Camera实例
        this.cameraId = cameraId ;//标记当前使用的cameraId
        getCameraManager(context).openCamera(cameraId, inStateCallback, getHandle());
    }

    //打开相机，优先使用缓存，提高camera转换效率
    @SuppressLint("MissingPermission")
    public void openCamera(@NonNull Context context, @NonNull String cameraId, CameraDevice.StateCallback stateCallback) throws CameraAccessException {
        //若当前存在已经打开的相机实例，且方向一致，则直接返回实例
        if(cameraDevice != null && cameraId.equals(this.cameraId)){
            stateCallback.onOpened(cameraDevice);
        }else{
            outStateCallback = stateCallback ;
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
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    //关闭打开的CaptureSession实例
    private void closeCaptureSession(){
        if(captureSession != null){
            captureSession.close();
            captureSession = null;
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
            //相机打开成功
            cameraDevice = camera ;

            if(outStateCallback != null){
                outStateCallback.onOpened(cameraDevice);
            }
            Log.i(TAG,"camera opened");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice = null ;
            if(outStateCallback != null){
                outStateCallback.onDisconnected(camera);
            }
            Log.e(TAG,"camera disconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null ;
            if(outStateCallback != null){
                outStateCallback.onError(camera,error);
            }else{
                Log.e(TAG,"outStateCallback = null");
            }
            Log.e(TAG,"open camera error:error state =" + error);
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            super.onClosed(camera);
            Log.e(TAG,"camera device closed");
        }
    };

}
