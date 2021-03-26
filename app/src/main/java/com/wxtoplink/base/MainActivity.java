package com.wxtoplink.base;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;

import com.xuxianpu.tools.camera.CameraComponent;
import com.xuxianpu.tools.camera.impl.CameraTakePhotoImpl;
import com.xuxianpu.tools.camera.impl.NoViewPreviewCamera;
import com.xuxianpu.tools.camera.impl.ViewPreviewCamera;
import com.xuxianpu.tools.camera.interfaces.CameraTakePhoto;
import com.xuxianpu.tools.camera.interfaces.PreviewDataCallBack;
import com.xuxianpu.tools.camera.utils.CameraUtils;


public class MainActivity extends AppCompatActivity {

    CameraComponent cameraComponent = CameraComponent.getInstance();

    private static final String TAG = MainActivity.class.getSimpleName();


    private TextureView cameraView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = findViewById(R.id.camera_view);
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            normalCameraUseDemo();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraComponent.safeRelease();
    }


    private void normalCameraUseDemo() throws CameraAccessException{
        CameraTakePhotoImpl cameraInstance = (CameraTakePhotoImpl) cameraComponent.getCamera(CameraComponent.CameraType.NORMAL, this);
        cameraInstance.setSurfaceView(cameraView);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw new RuntimeException("permission define by android.permission.CAMERA");
        }
        cameraInstance.startPreview();
    }

    /**
     * 无界面预览调用示例，无相机界面显示，可以后台运行，实施捕获相机预览数据输出
     * @throws CameraAccessException
     */
    private void previewNoDataUseDemo() throws CameraAccessException {
        NoViewPreviewCamera cameraInstance = (NoViewPreviewCamera) cameraComponent.getCamera(CameraComponent.CameraType.NO_VIEW_PREVIEW_DATA, this);
        cameraInstance.setPreviewDataCallBack(new PreviewDataCallBack() {
            @Override
            public void previewData(Image image) {
                Log.i(TAG, "previewData" + image);
                image.close();
            }

            @Override
            public void errorCallBack(Exception e) {

            }

            @Override
            public void surfaceSizeChanged(Size size) {

            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw new RuntimeException("permission define by android.permission.CAMERA");
        }
        cameraInstance.startPreview();
    }

    /**
     * 一边预览，一遍回调数据流处理调用示范
     * @throws CameraAccessException
     */
    private void previewCameraUseDemo() throws CameraAccessException {
        //获取对应功能的实例
        ViewPreviewCamera cameraInstance = (ViewPreviewCamera) cameraComponent.getCamera(CameraComponent.CameraType.VIEW_PREVIEW_DATA, this);
        //设置预览输出的纹理表面
        cameraInstance.setSurfaceView(cameraView);
        //设置相机，前置Or后置，不设置，则使用默认相机ID
        cameraInstance.setCameraId(CameraUtils.getDefaultCameraId(this));
        //设置预览数据回调
        cameraInstance.setPreviewDataCallBack(new PreviewDataCallBack() {
            @Override
            public void previewData(Image image) {
                Log.i(TAG, "previewData" + image);
                image.close();
            }

            @Override
            public void errorCallBack(Exception e) {

            }

            @Override
            public void surfaceSizeChanged(Size size) {

            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw new RuntimeException("permission define by android.permission.CAMERA");
        }
        //开启预览
        cameraInstance.startPreview();
    }
}
