package com.xuxianpu.tools.camera.utils;

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
 * Camear工具类
 * Created by 12852 on 2018/8/28.
 */

public class CameraUtils {

    private static final String TAG = CameraUtils.class.getSimpleName() ;

    /**
     * 获取相机的合适大小
     * @param context 上下文
     * @param cameraId 相机方向
     * @param format 数据格式
     * @param maxSize 允许的最大大小
     * @return 最适宜的大小
     */
    public static Size getFitSize(@NonNull Context context,@Nullable String cameraId,@NonNull int format,@Nullable Size maxSize){
        CameraManager cameraManager ;

        try {
            if(cameraId == null){
                //获取默认相机方向
                cameraId = getDefaultCameraId(context);
            }
            cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //获取支持的输出大小
            Size[] supportSize = map.getOutputSizes(format);
            if(supportSize == null){
                throw new IllegalArgumentException(String.format(
                        "format 0x%x was not defined in either ImageFormat or PixelFormat", format));
            }
            return getFitSize(Arrays.asList(supportSize),maxSize);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return null ;
    }

    /**
     * 获取合适大小
     * @param sizeList 大小列表
     * @param maxSize 允许最大大小
     * @return 最合适的大小
     */
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
            Log.i(TAG,"size :{ height =" + size.getHeight() + " ,width =" + size.getWidth() + " };");
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
            return 0;
        };
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
    public static String getDefaultCameraId(@NonNull Context context) throws CameraAccessException{
        String [] cameraList = null;
        //获取CameraID
        cameraList = getCameraList(context);

        if(cameraList.length > 0){
            //设置默认相机ID
            return cameraList[0] ;
        }else{
            throw new IllegalArgumentException("The device does not support cameras");
        }
    }

    //获取相机的设备列表
    public static String [] getCameraList(@NonNull Context context) throws CameraAccessException{
        return ((CameraManager) context.getApplicationContext().getSystemService(Context.CAMERA_SERVICE)).getCameraIdList();
    }

    //校验相机ID是否可用
    public static boolean checkCameraId(@NonNull Context context,@NonNull String cameraId) throws CameraAccessException {

        String [] supportCameraList = getCameraList(context);

        for(String supportCameraId : supportCameraList){
            //判断相机方向是否匹配
            if(supportCameraId.equals(cameraId)){
                return true ;
            }
        }
        return false ;

    }

    /**
     * 保存图片
     * @param image JPEG数据格式图片
     * @param path  保存路径
     * @return true,保存成功
     */
    public static boolean saveImage(@NonNull Image image ,@NonNull String path){
        if(image.getFormat() != ImageFormat.JPEG){
            throw new IllegalArgumentException("This method only supports ImageFormat.JPEG format");
        }else{
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            byte [] data = new byte[byteBuffer.capacity()];
            //将图片数据存到data中
            byteBuffer.get(data);
            //保存图片
            return saveImage(data,path);
        }
    }

    /**
     * 保存图片
     * @param data jpeg数据流
     * @param path  保存路径
     * @return true ,保存成功
     */
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
