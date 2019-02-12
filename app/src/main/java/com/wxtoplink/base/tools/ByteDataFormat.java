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
        byte [] nv21 ;
        Image.Plane yPlane = image.getPlanes()[0];//y分量，显示亮度
        Image.Plane uPlane = image.getPlanes()[1];//U分量，与V分量一同决定色彩
        Image.Plane vPlane = image.getPlanes()[2];//V分量，与U分量一同决定色彩

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();

        int pixelStride = uPlane.getPixelStride();//像素跨步，用于区分Planar(步数为1)环境，以及SemiPlanar(步数为2)环境

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + ySize/2];//nv21数据是y分量的1.5倍

        byte[] uByte = new byte[uSize];
        byte[] vByte = new byte[vSize];
        yBuffer.get(nv21,0,ySize);//填充Y部分
        vBuffer.get(vByte);
        uBuffer.get(uByte);
        if(pixelStride == 1){//步伐等于1，说明，uv已经分离
            for(int i = 0; i < ySize/2 ; i = i + 2){
                nv21[ySize + i] = vByte[i/2];
                nv21[ySize + i + 1] = uByte[i/2];
            };

        }else{//像素步伐为2，说明uv尚未分离,uv交错
            for(int i = 0 ;i < uSize;i= i + 2){
                nv21[ySize + i] = vByte[i];
                nv21[ySize + i + 1] = uByte[i];
            };
            //高效方式，Android 不保证UV分量一定正确输出
//            vBuffer.get(nv21,ySize,vSize);
        }

        return nv21;
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


    /**
     * 所有方法传递的数据格式需要为JPEG格式
     */
    public static final class JPEGTransform{

        //变换
        public static byte[] transformBytes(byte[] jpegByte,Matrix matrix){
            Bitmap bitmap = conventByte2Bitmap(jpegByte);
            return conventBitmap2Bytes(transformBitmap(bitmap,matrix));
        }

        //变换
        public static Bitmap transformBitmap(Bitmap bitmap,Matrix matrix){
            return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        }

        /**
         * 图片数据按给定宽高进行缩放
         * @param jpegBytes Image data stream
         * @param width Target image width
         * @param height Target image height
         * @return
         */
        public static byte[] scalingBytes(byte[] jpegBytes,float width,float height){
            return conventBitmap2Bytes(scalingBitmap(conventByte2Bitmap(jpegBytes),width,height));
        }

        /**
         * 缩放bitmap
         * @param bitmap bitmap source
         * @param width Target width
         * @param height Target height
         * @return
         */
        public static Bitmap scalingBitmap(Bitmap bitmap,float width,float height){
            Matrix matrix = new Matrix();
            float currentWidth = bitmap.getWidth();
            float currentHeight = bitmap.getHeight();
            float scaleX = width/currentWidth ;
            float scaleY = height/currentHeight ;
            matrix.postScale(scaleX,scaleY);
            return transformBitmap(bitmap,matrix);
        }

        //翻转图片
        public static byte[] rotateBytes(byte[] jpegBytes, int rotate) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);

            return transformBytes(jpegBytes,matrix);
        }

        public static Bitmap rotateBitmap(Bitmap bitmap,int rotate){
            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            return transformBitmap(bitmap,matrix);
        }

        //将byte转换为Bitmap
        public static Bitmap conventByte2Bitmap(byte[] jpegBytes){
            return BitmapFactory.decodeByteArray(jpegBytes,0,jpegBytes.length);
        }

        //将bitmap转换为byte
        public static byte[] conventBitmap2Bytes(Bitmap bitmap){
            ByteArrayOutputStream ops = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,ops);
            return ops.toByteArray();
        }
    }

}
