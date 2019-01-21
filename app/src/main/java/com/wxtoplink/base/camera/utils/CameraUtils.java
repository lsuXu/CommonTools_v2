package com.wxtoplink.base.camera.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by 12852 on 2018/8/28.
 */

public class CameraUtils {

    private static final String TAG = CameraUtils.class.getSimpleName() ;

    public static Size getFitSize(@NonNull Context context,@Nullable String cameraId,@NonNull int format,@Nullable Size maxSize){
        CameraManager cameraManager ;

        try {
            if(cameraId == null){
                cameraId = getDefaultCameraId(context);
            }
            cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] supportSize = map.getOutputSizes(format);
            if(supportSize == null){
                throw new IllegalArgumentException(String.format(
                        "format 0x%x was not defined in either ImageFormat or PixelFormat", format));
            }
            return CameraUtils.getFitSize(Arrays.asList(supportSize),maxSize);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return null ;
    }

    public static Size getFitSize(@NonNull List<Size> sizeList,@Nullable Size maxSize){
        /**
         * 基于图片的区域( 宽 * 高 )，比较它们的大小，进行排序
         */
        final Comparator<Size> compareSizesByArea = new Comparator<Size>() {

            @Override
            public int compare(Size lhs, Size rhs) {
                return Long.signum((long) rhs.getWidth() * rhs.getHeight() -
                        (long) lhs.getWidth() * lhs.getHeight());
            }

        };

        //对预览尺寸进行排序（从大到小）
        Collections.sort(sizeList,compareSizesByArea);

        if(maxSize == null){
            //返回最大的尺寸
            return sizeList.get(0);
        }

        for(Size size : sizeList ){
            Log.i("mat","size :{ height =" + size.getHeight() + " ,width =" + size.getWidth() + " };");
            if(size.getWidth()*size.getHeight()<=maxSize.getHeight()*maxSize.getWidth()){
                return size ;
            }
        }
        //返回最小的尺寸
        return sizeList.get(sizeList.size()-1);
    }

    //获取相机预览界面的翻转角度，用于纠正相机预览的方向
    public static int getOrientation(@NonNull Context context,@NonNull String cameraId){
        if(!(context instanceof Activity)){
            Log.i(TAG,"active instanceof Activity = false");
            return 0;
        };
        Log.i(TAG,"active instanceof Activity = true");
        try {
            final SparseIntArray ORIENTATIONS = new SparseIntArray();
            ORIENTATIONS.append(Surface.ROTATION_0, 90);
            ORIENTATIONS.append(Surface.ROTATION_90, 0);
            ORIENTATIONS.append(Surface.ROTATION_180, 270);
            ORIENTATIONS.append(Surface.ROTATION_270, 180);
            int rotation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();

            //获取相机服务
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            //获取对应相机的特征列表
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

            //获取摄像机传感器的定位
            int mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            int orientation = (ORIENTATIONS.get(rotation) +mSensorOrientation + 270 )%360;
            return orientation ;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }


    //获取一个可用的相机ID
    public static String getDefaultCameraId(@NonNull Context context) throws CameraAccessException {
        String [] cameraList = null;
        //获取CameraID
        cameraList = ((CameraManager) context.getApplicationContext().getSystemService(Context.CAMERA_SERVICE)).getCameraIdList();

        if(cameraList.length > 0){
            //设置默认相机ID
            return cameraList[0] ;
        }else{
            throw new IllegalArgumentException("The device does not support cameras");
        }
    }

    //校验相机ID是否可用
    public static boolean checkCameraId(@NonNull Context context,@NonNull String cameraId) throws CameraAccessException {

        String [] supportCameraList = null;

        supportCameraList = ((CameraManager) context.getApplicationContext().getSystemService(Context.CAMERA_SERVICE)).getCameraIdList();

        for(String supportCameraId : supportCameraList){
            if(supportCameraId.equals(cameraId)){
                return true ;
            }
        }
        return false ;

    }

    public static boolean saveImage(@NonNull Image image ,@NonNull String path){
        if(image.getFormat() != ImageFormat.JPEG){
            throw new IllegalArgumentException("This method only supports ImageFormat.JPEG format");
        }else{
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            byte [] data = new byte[byteBuffer.capacity()];
            byteBuffer.get(data);
            return saveImage(data,path);
        }
    }

    public static boolean saveImage(@NonNull byte[] data ,@NonNull String path){
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(path);
            output.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (null != output) {
                try {
                    output.flush();
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

}
