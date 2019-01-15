package com.wxtoplink.base.camera.impl;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.wxtoplink.base.camera.interfaces.CameraListener;
import com.wxtoplink.base.camera.interfaces.CameraStatusListener;
import com.wxtoplink.base.camera.interfaces.CameraTemplate;
import com.wxtoplink.base.camera.utils.CameraUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * 常用相机类，用于预览拍照使用
 * Created by 12852 on 2018/8/28.
 */

public abstract class CameraTemplateImpl implements CameraTemplate,CameraListener {

    private static final String TAG = CameraTemplateImpl.class.getSimpleName();

    public CameraTemplateImpl(Context context) {
        this.context = context;
    }

    private boolean isPreview = false;

    protected Size maxSize ;

    protected int format ;

    protected CameraManager cameraManager;

    protected Context context;

    protected String cameraId;

    private CameraDevice cameraDevice;

    //捕获请求的构造类，是CaptureRequest的工厂类
    protected CaptureRequest.Builder captureRequestBuilder;

    //捕获请求，CameraCaptureSession进行捕获操作的
    protected CaptureRequest captureRequest;

    protected CameraCaptureSession cameraCaptureSession;

    private HandlerThread mBackgroundThread;

    private Handler mBackgroundHandler;

    private List<CameraStatusListener> cameraStatusListeners = null;

    @RequiresPermission(android.Manifest.permission.CAMERA)
    @Override
    public synchronized void startPreview() {
        Log.i(TAG, "camera startPreview:" + isPreview);
        if(cameraStatusListeners == null){
            cameraStatusListeners = new ArrayList<>();
        }
        if(cameraId == null){
            try {
                cameraId = CameraUtils.getDefaultCameraId(context);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        if (!isPreview) {
            isPreview = true;
            startBackgroundThread();
            if(cameraManager == null) {
                cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            }

            //打开相机，再CameraDevice.StateCallback回调中监听相机打开状态
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //权限不允许
                isPreview = false ;
                throw new SecurityException("Manifest.permission.CAMERA not allowed");
            }
            try {
                //打开相机，在stateCallback中进行下一步
                cameraManager.openCamera(cameraId, stateCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

    }

    //停止相机预览
    @Override
    public synchronized void stopPreview() {
        if(isPreview){
            isPreview = false ;
            synchronized (this){
                if (cameraCaptureSession != null) {
                    synchronized (cameraCaptureSession) {
                        cameraCaptureSession.close();
                        cameraCaptureSession = null;
                    }
                }
                if (cameraDevice != null) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            }
            stopBackgroundThread();
        }
    }

    @Override
    public void setCameraId(String cameraId) {
        this.cameraId = cameraId ;
    }

    @Override
    public void setImageFormat(int imageFormat) {
        this.format = imageFormat ;
    }

    @Override
    public void setMaxSize(Size maxSize) {
        this.maxSize = maxSize ;
    }

    @Override
    public boolean isPreview(){
        return isPreview ;
    }

    //创建相机预览会话
    private void createCameraPreviewSession() {
        if(isPreview) {
            synchronized (CameraDevice.class) {
                synchronized (ImageReader.class) {
                    try {
                        //创建新的捕获请求，高帧率优先
                        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                        //无界面预览，在这里处理，不加入预览界面的surface,则不会输出预览界面
                        for(Surface surface: getSurfaceList()){
                            captureRequestBuilder.addTarget(surface);
                        }
                        //CameraDevice创建一个会话请求，再指定SurfaceView中进行绘制，再CameraCaptureSession.StateCallback中监听创建请求的结果并做相应配置
                        cameraDevice.createCaptureSession(getTotalSurfaceList(), captureStateCallback, mBackgroundHandler);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 启动一个后台线程以及它的Handle
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {

        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * 停止后台线程以及它的Handle
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if(mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    //预设将会用到的所有获取显示图像数据的预览目标surface列表
    public abstract List<Surface> getTotalSurfaceList();

    //初始状态的surface列表
    public abstract List<Surface> getSurfaceList();

        /*----相机的回调处理----*/

    //用于接收相机设备状态的回掉
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //相机打开成功
            cameraDevice = camera ;
            for(CameraStatusListener cameraStatusListener :cameraStatusListeners){//状态回调
                cameraStatusListener.onOpened();
            }
            //创建相机预览会话
            createCameraPreviewSession();
            Log.i(TAG,"camera opened");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice = null ;
            isPreview = false ;
            Log.e(TAG,"camera disconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null ;
            for (CameraStatusListener cameraStatusListener: cameraStatusListeners){
                cameraStatusListener.onOpenError();
            }
            stopPreview();
            Log.e(TAG,"open camera error:error state =" + error);
        }
    };

    //用于接收关于摄像机捕获会话状态的更新的回调对象
    private final CameraCaptureSession.StateCallback captureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            //若相机失去连接或以关闭
            if(cameraDevice == null) {
                isPreview = false ;
                return;
            }
            for (CameraStatusListener cameraStatusListener: cameraStatusListeners){//状态回调
                cameraStatusListener.onConfigured();
            }
            //创建成功的CameraCaptureSession
            cameraCaptureSession = session ;

            synchronized (cameraCaptureSession) {
                synchronized (CaptureRequest.Builder.class) {
                    //设置以最快速度进行对焦
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    //设置预览视图的旋转方向
                    captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, CameraUtils.getOrientation(context,cameraId));
                    //创建捕获请求
                    captureRequest = captureRequestBuilder.build();

                    try {
                        //执行图像捕捉的请求，并在CameraCaptureSession.CaptureCallback中得到回调（ps:ImageRead 的surfaceView获取到的数据在ImageRead的回调中获取）
                        cameraCaptureSession.setRepeatingRequest(captureRequest, captureCallback, mBackgroundHandler);
                    } catch (Exception e){
                        Log.e(TAG, "camera Configure error:" + e.getMessage());
                        e.printStackTrace();
                    };
                }
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            //相机配置会话失败
            session.close();
            for (CameraStatusListener cameraStatusListener: cameraStatusListeners){//状态回调
                cameraStatusListener.onConfigureFailed();
            }
            Log.e(TAG,"camera ConfigureFailed error:error state");
        }
    };

    protected final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            for (CameraStatusListener cameraStatusListener: cameraStatusListeners){//状态回调
                cameraStatusListener.onCaptureStarted();
            }
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            for (CameraStatusListener cameraStatusListener: cameraStatusListeners){//状态回调
                cameraStatusListener.onCaptureCompleted();
            }
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            Log.e(TAG,"onCaptureFailed :failure =" + failure);
            for (CameraStatusListener cameraStatusListener: cameraStatusListeners){//状态回调
                cameraStatusListener.onCaptureFailed();
            }
            super.onCaptureFailed(session, request, failure);
        }

    };


    /** 相机状态回调管理 **/

    @Override
    public boolean addListener(CameraStatusListener cameraStatusListener) {
        if(cameraStatusListeners == null){
            cameraStatusListeners = new ArrayList<>();
        }
        return cameraStatusListeners.add(cameraStatusListener);
    }

    @Override
    public boolean removeListener(CameraStatusListener cameraStatusListener) {
        if(cameraStatusListeners == null ){
            return false ;
        }

        return cameraStatusListeners.remove(cameraStatusListener);
    }

    @Override
    public Iterable<CameraStatusListener> getListenerQueue() {
        return cameraStatusListeners;
    }

    @Override
    public void removeAllListener() {
        if(cameraStatusListeners != null){
            cameraStatusListeners.clear();
        }
    }
}
