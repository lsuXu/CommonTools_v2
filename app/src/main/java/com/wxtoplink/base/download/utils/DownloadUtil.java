package com.wxtoplink.base.download.utils;

import com.wxtoplink.base.download.DownloadTask;
import com.wxtoplink.base.log.DownloadLog;
import com.wxtoplink.base.tools.EncryptionCheckUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * Created by 12852 on 2019/2/28.
 */

public class DownloadUtil {

    private static final String MD5_DEFAULT_VALUE = "null";

    //生成断点重传标记的文件名
    public static String generateBreakFilePath(DownloadTask downloadTask){
        return String.format("%s_%s",downloadTask.getFile_path(),getBreakPoint(downloadTask));
    }

    //获取断点文件标识(主要用于区分是否是相同文件，通过md5标识)
    public static String getBreakPoint(DownloadTask downloadTask){
        return breakSupport(downloadTask) ? downloadTask.getMd5():MD5_DEFAULT_VALUE;
    }

    //md5，被用于判断是否支持断点续传
    public static boolean breakSupport(DownloadTask downloadTask){
        return downloadTask.getMd5() != null && downloadTask.getMd5().length() > 0 ;
    }

    //获取文件断点位置
    public static long getRangeStart(DownloadTask downloadTask){
        long startRange = 0 ;
        if(breakSupport(downloadTask)){
            String breakFilePath = generateBreakFilePath(downloadTask);
            File file = new File(breakFilePath);
            if(file.exists()){
                startRange = file.length();
            }
        }

        return startRange ;
    }

    public static boolean writeFile(InputStream inputStream, String filePath) {
        return writeFile(inputStream,filePath,0);
    }

        /**
         * 写文件
         * @param inputStream 文件输入流
         * @param filePath  文件路径
         * @param startRange    写入开始范围
         * @return
         * @throws IOException
         */
    public static boolean writeFile(InputStream inputStream, String filePath,long startRange){

        boolean downloadSuccess = false;
        RandomAccessFile randomAccessFile = null;

        //若是从头开始写，则先删除源文件
        if(startRange == 0){
            File file = new File(filePath);
            if(file.exists()){
                file.delete();
            }
        }

        try {
            byte[] buf = new byte[2048];
            randomAccessFile = new RandomAccessFile(filePath, "rwd");
            randomAccessFile.seek(startRange);//寻找定位到文件结尾

            int len = 0;
            while ((len = inputStream.read(buf)) != -1) {
                randomAccessFile.write(buf, 0, len);//写入文件
            }
            downloadSuccess = true ;
        } catch (Exception e) {
            DownloadLog.getInstance().e("DownloadUtil","download error",e);
            e.printStackTrace();
        } finally {
            try {
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }

                if (inputStream != null) {
                    inputStream.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return downloadSuccess ;

    }

    //校验md5是否相同
    public static boolean checkMd5(String filePath ,String md5){
        String fileMd5 = EncryptionCheckUtil.md5sum(filePath);
        if(fileMd5 != null){
            return fileMd5.equalsIgnoreCase(md5);
        }
        return false ;
    }
}
