package com.wxtoplink.base.camera.impl;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.view.Surface;

import com.wxtoplink.base.camera.Camera2Holder;
import com.wxtoplink.base.camera.CameraLog;
import com.wxtoplink.base.camera.interfaces.CameraStatusListener;
import com.wxtoplink.base.camera.interfaces.CameraTemplate;
import com.wxtoplink.base.camera.utils.CameraUtils;

import java.util.List;


/**
 * 常用相机类，用于预览拍照使用
 * Created by 12852 on 2018/8/28.
 */

public abstract class CameraTemplateImpl implements CameraTemplate {

    private final String TAG ;

    CameraTemplateImpl(Context context) {
        TAG = CameraTemplateImpl.this.getClass().toString() ;
        this.context = context;
    }

    private boolean isPreview = false;//是否正在预览

    private Size previewMaxSize ;//允许的最大预览大小

    private Size previewFitSize ;//合适的预览大小

    int previewFormat ;//预览格式

    Context context;

    private String cameraId;//相机ID

    private CameraDevice cameraDevice;

    //捕获请求的构造类，是CaptureRequest的工厂类
    CaptureRequest.Builder captureRequestBuilder;

    CameraCaptureSession cameraCaptureSession;

    private HandlerThread mBackgroundThread;

    private Handler mBackgroundHandler;

    private CameraStatusListener cameraStatusListener = null;

    private Handler hostHandler ;

    @Override
    public void openCamera() {
        CameraLog.i(TAG,"openCamera()");

        if (!isPreview) {
            isPreview = true;
            //打开相机，再CameraDevice.StateCallback回调中监听相机打开状态
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //权限不允许
                isPreview = false ;
                throw new SecurityException("Manifest.permission.CAMERA not allowed");
            }
            try {
                //打开相机，在stateCallback中进行下一步
                Camera2Holder.getInstance().openCamera(context,getCameraId(), stateCallback);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }else{
            if(cameraDevice != null){
                //创建相机预览会话
                createCameraPreviewSession();
            }
        }
    }

    //获取需要打开的相机ID
    String getCameraId(){
        if(cameraId == null){
            try {
                cameraId = CameraUtils.getDefaultCameraId(context);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        return cameraId ;
    }

    //停止相机预览
    @Override
    public synchronized void stopPreview() {
        CameraLog.i(TAG,"stopPreview()");
        if(isPreview){
            isPreview = false ;
            if (cameraCaptureSession != null) {
                synchronized (cameraCaptureSession) {
                    try {
                        cameraCaptureSession.abortCaptures();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    cameraCaptureSession.close();
                    cameraCaptureSession = null;
                }
            }
            if (cameraDevice != null) {//CameraDevice的创建以及销毁由Camera2holder完成，这里仅需要释放对CameraDevice的引用
                cameraDevice = null;
            }
            hostHandler = null;
            stopBackgroundThread();
        }
    }

    @Override
    public void setCameraId(String cameraId) {
        this.cameraId = cameraId ;
    }

    @Override
    public void setPreviewFormat(int previewFormat) {
        this.previewFormat = previewFormat ;
    }

    @Override
    public void setPreviewMaxSize(Size previewMaxSize) {
        this.previewMaxSize = previewMaxSize ;
    }

    @Override
    public boolean isPreview(){
        return isPreview ;
    }

    Handler getHandle(){
        if(mBackgroundHandler == null){
            startBackgroundThread();
        }
        return mBackgroundHandler;
    }

    @Override
    public Size getPreviewFitSize () {
        if(previewFitSize == null) {
            previewFitSize = CameraUtils.getFitSize(context,getCameraId(),previewFormat, previewMaxSize);
        }
        return previewFitSize;
    }

    //创建相机预览会话
    private void createCameraPreviewSession() {
        CameraLog.i(TAG,"createCameraPreviewSession()");
        if(isPreview) {
            try {
                //创建新的捕获请求，高帧率优先
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                //初始化Surface
                initSurface();

                //无界面预览，在这里处理，不加入预览界面的surface,则不会输出预览界面
                for(Surface surface: getPreviewSurfaceList()){
                    captureRequestBuilder.addTarget(surface);
                }
                //CameraDevice创建一个会话请求，再指定SurfaceView中进行绘制，再CameraCaptureSession.StateCallback中监听创建请求的结果并做相应配置
                cameraDevice.createCaptureSession(getPresetSurfaceList(), captureStateCallback, mBackgroundHandler);

            } catch (final Exception e) {
                CameraLog.e(TAG,"createCameraPreviewSession error : " + e.getMessage());
                e.printStackTrace();
                //引发异常情况，客户端可以通过释放相机资源后重启
                if(cameraStatusListener != null){
                    executeToHostThread(new Runnable() {
                        @Override
                        public void run() {
                            if(cameraStatusListener != null)
                                cameraStatusListener.onError(e);
                        }
                    });
                }
            }
        }
    }

    /**
     * 启动一个后台线程以及它的Handle
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        CameraLog.i(TAG,"startBackgroundThread()");
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * 停止后台线程以及它的Handle
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        CameraLog.i(TAG,"stopBackgroundThread()");

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
    public abstract List<Surface> getPresetSurfaceList();

    //初始状态的surface列表
    public abstract List<Surface> getPreviewSurfaceList();

        /*----相机的回调处理----*/

    //用于接收相机设备状态的回掉
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            CameraLog.i(TAG,"camera opened");
            //相机打开成功
            cameraDevice = camera ;
            if(cameraStatusListener != null){
                executeToHostThread(new Runnable() {
                    @Override
                    public void run() {
                        if(cameraStatusListener != null)
                            cameraStatusListener.onOpened();
                    }
                });
            }
            //创建相机预览会话
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            CameraLog.e(TAG,"camera disconnected");
            cameraDevice = null ;
            isPreview = false ;
            if(cameraStatusListener != null){
                executeToHostThread(new Runnable() {
                    @Override
                    public void run() {
                        if(cameraStatusListener != null)
                            cameraStatusListener.onDisconnected();
                    }
                });
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera,final int error) {
            CameraLog.e(TAG,"open camera error:error status =" + error);
            if(cameraStatusListener != null){
                executeToHostThread(new Runnable() {
                    @Override
                    public void run() {
                        if(cameraStatusListener != null)
                            cameraStatusListener.onError(new Throwable("open camera error: status = " + error));
                    }
                });
            }
        }
    };

    //用于接收关于摄像机捕获会话状态的更新的回调对象
    private final CameraCaptureSession.StateCallback captureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            CameraLog.i(TAG, "CameraCaptureSession onConfigured()");
            //若相机失去连接或以关闭
            if(cameraDevice == null) {
                isPreview = false ;
                return;
            }
            if(cameraStatusListener != null){
                executeToHostThread(new Runnable() {
                    @Override
                    public void run() {
                        if(cameraStatusListener != null)
                            cameraStatusListener.onConfigured();
                    }
                });
            }
            //创建成功的CameraCaptureSession
            cameraCaptureSession = session ;

            synchronized (cameraCaptureSession) {
                //设置以最快速度进行对焦
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //设置预览视图的旋转方向
                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, CameraUtils.getOrientation(context,getCameraId()));

                try {
                    //执行图像捕捉的请求，并在CameraCaptureSession.CaptureCallback中得到回调（ps:ImageRead 的surfaceView获取到的数据在ImageRead的回调中获取）
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), captureCallback, getHandle());
                } catch (Exception e){
                    CameraLog.e(TAG, "CameraCaptureSession Configure error:" + e.getMessage());
                    e.printStackTrace();
                };
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            CameraLog.e(TAG,"CameraCaptureSession onConfigureFailed()");
            //相机配置会话失败
            session.close();
            cameraCaptureSession = null ;
            if(cameraStatusListener != null){
                executeToHostThread(new Runnable() {
                    @Override
                    public void run() {
                        if(cameraStatusListener != null)
                            cameraStatusListener.onConfigureFailed();
                    }
                });
            }
            isPreview = false ;
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            super.onClosed(session);
            CameraLog.e(TAG,"CameraCaptureSession onClosed()");
            session.close();
        }
    };


    protected final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            if(cameraStatusListener != null){
                executeToHostThread(new Runnable() {
                    @Override
                    public void run() {
                        if(cameraStatusListener != null)
                            cameraStatusListener.onCaptureStarted();
                    }
                });
            }
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            if(cameraStatusListener != null){
                executeToHostThread(new Runnable() {
                    @Override
                    public void run() {
                        if(cameraStatusListener != null)
                            cameraStatusListener.onCaptureCompleted();
                    }
                });
            }
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            CameraLog.e(TAG,String.format("onCaptureFailed :failure = %s",failure.getReason()));

            if(cameraStatusListener != null){
                executeToHostThread(new Runnable() {
                    @Override
                    public void run() {
                        if(cameraStatusListener != null)
                            cameraStatusListener.onCaptureFailed();
                    }
                });
                if(failure.getReason() == CaptureFailure.REASON_ERROR){//由于内部错误引起捕获失败，则关闭预览，触发错误回调
                    executeToHostThread(new Runnable() {
                        @Override
                        public void run() {
                            if(cameraStatusListener != null)
                                cameraStatusListener.onError(new Throwable("The CaptureResult has been dropped this frame due to an error in the framework"));
                        }
                    });
                }
            }
            super.onCaptureFailed(session, request, failure);
        }

    };

    @Override
    public void setCameraStatusListener(CameraStatusListener cameraStatusListener) {
        this.cameraStatusListener = cameraStatusListener ;
    }

    //获取宿主线层的Looper
    void setHostHandler(){
        if(Looper.myLooper() != null) {
            hostHandler = new Handler(Looper.myLooper());
        }else{
            hostHandler = new Handler(Looper.getMainLooper());
        }
    }

    void executeToHostThread(Runnable runnable){
        if(hostHandler != null) {
            hostHandler.post(runnable);
        }
    }

}
