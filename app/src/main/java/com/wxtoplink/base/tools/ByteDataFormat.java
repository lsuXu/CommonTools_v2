package com.wxtoplink.base.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.support.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * 图片数据转换工具类
 * Created by 12852 on 2018/6/7.
 */

public class ByteDataFormat {

    /**
     * Convert image format
     * only support ImageFormat.NV21 and ImageFormat.YUY2 for now
     * @param data Image data stream
     * @param format    Image format
     * @param width     Image weight
     * @param height    Image height
     * @return      Image data stream in Jpge data format
     */
    public static byte [] getJpegByte(@NonNull byte[] data,@NonNull int format,@NonNull int width,@NonNull int height,@NonNull Rect cutRect){
        ByteArrayOutputStream ops = new ByteArrayOutputStream();
        YuvImage image = new YuvImage(data,format,width,height,null);
        image.compressToJpeg(cutRect, 80, ops);
        return ops.toByteArray();
    }

    /**
     * Convert image format from NV21 to JPEG
     * @param data Image data stream
     * @param width Image weight
     * @param height    Image height
     * @return  Image data stream in Jpeg data format
     */
    public static byte [] NV21_2_JPEG(@NonNull byte[] data,@NonNull int width,@NonNull int height){
        return getJpegByte(data, ImageFormat.NV21,width,height,new Rect(0, 0, width, height));
    }

    /**
     * Convert image format from YUY2 to JPEG
     * @param data Image data stream
     * @param width Image weight
     * @param height    Image height
     * @return  Image data stream in Jpeg data format
     */
    public static byte [] YUY2_2_JPEG(@NonNull byte[] data,@NonNull int width,@NonNull int height){
        return getJpegByte(data, ImageFormat.YUY2,width,height,new Rect(0, 0, width, height));
    }
    /**
     * Convert image format from NV21 to JPEG
     * @param data Image data stream
     * @param width Image weight
     * @param height    Image height
     * @param cutRect Image cutting range
     * @return  Image data stream in Jpeg data format
     */
    public static byte [] NV21_2_JPEG(@NonNull byte[] data,@NonNull int width,@NonNull int height,@NonNull Rect cutRect){
        /*if(cutRect == null){
            throw new IllegalArgumentException("Rect must be nut null");
        }*/
        return getJpegByte(data, ImageFormat.NV21,width,height,cutRect);
    }

    /**
     * Convert image format from YUY2 to JPEG
     * @param data Image data stream
     * @param width Image weight
     * @param height    Image height
     * @param cutRect Image cutting range
     * @return  Image data stream in Jpeg data format
     */
    public static byte [] YUY2_2_JPEG(@NonNull byte[] data,@NonNull int width,@NonNull int height,@NonNull Rect cutRect){
        return getJpegByte(data, ImageFormat.YUY2,width,height,cutRect);
    }

    /**
     * 将camera2相机预览数据获取到的YUV420_888格式image数据转换拼接为NV21格式byte数据
     * @param image YUV420_888 format image
     * @return Image data stream in NV21 data format
     */
    public static byte[] formatYUV420_888ToNV21(@NonNull Image image){
        if(image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("only support ImageFormat.YUV_420_888");
        }
        Image.Plane[] planes = image.getPlanes(); // in YUV420_888 format
        int acc = 0;
        ByteBuffer[] buff = new ByteBuffer[planes.length];

        for (int i = 0; i < planes.length; i++) {
            buff[i] = planes[i].getBuffer();
            acc += buff[i].capacity();
        }
        byte[] data = new byte[acc],
                tmpCb = new byte[buff[1].capacity()] , tmpCr = new byte[buff[2].capacity()];

        buff[0].get(data, 0, buff[0].capacity()); // Y
        acc = buff[0].capacity();

        buff[2].get(tmpCr, 0, buff[2].capacity()); // Cr-V
        buff[1].get(tmpCb, 0, buff[1].capacity()); // Cb-U

        for(int i = 0; i <tmpCb.length;i++){
            data[acc] = tmpCr[i];
            data[acc +1] = tmpCb[i];
            acc = acc + 2 ;
        }

        return data ;
    }

    /**
     * 将JPEG格式的图片数据流 定高等比例按缩放
     * @param data image data stream for JPEG data format
     * @param targetHeight target image height
     * @return
     */
    public static byte[] getConstantHeightScaling(@NonNull byte [] data,@NonNull int targetHeight){

        Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        //按比例缩放后的高度
        int targetWidth = Math.round(width*targetHeight/ (float)height);

        Matrix matrix = new Matrix();
        float scaleWidth = (float) targetWidth/(float)width;
        float scaleHeight = (float)targetHeight/(float)height ;
        matrix.postScale(scaleWidth,scaleHeight);
        Bitmap newBmp = Bitmap.createBitmap(bitmap,0,0,width,height,matrix,true);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        newBmp.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] formatJPEG_NV21(int[] rgba,int width, int height){

        return null ;
    }

}
