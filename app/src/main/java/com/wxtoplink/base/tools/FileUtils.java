package com.wxtoplink.base.tools;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by 12852 on 2018/8/9.
 */

public class FileUtils {

    /**
     * Get the external storage directory path
     * @return root path
     */
    public static String getRootPath(){
        if(BaseUtils.sdCardExit()){
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return null ;
    }

    /**
     * delete file or folder
     * @param file
     */
    public static void deleteFiles(@NonNull File file){
        if(!file.exists()){
            return ;
        }else{
            if(file.isDirectory()){
                for(File childFile : file.listFiles()){
                    deleteFiles(childFile);
                }
            }else{
                file.delete();
            }
        }
    }


    /**
     * File rename
     * @param sourcePath source file path
     * @param targetPath target file path
     * @throws FileNotFoundException The source file does not exist
     */
    public static void renameFile(String sourcePath,String targetPath) throws FileNotFoundException {
        renameFile(new File(sourcePath),new File(targetPath));
    }

    /**
     * File rename
     * @param sourceFile source file
     * @param targetFile target file
     * @throws FileNotFoundException The source file does not exist
     */
    public static void renameFile(@NonNull File sourceFile,@NonNull File targetFile) throws FileNotFoundException {
        if(!sourceFile.exists()){
            throw new FileNotFoundException(String.format("file %s is not exist",sourceFile.getAbsolutePath()));
        }
        if(targetFile.exists()){
            targetFile.delete();
        }
        sourceFile.renameTo(targetFile);
    }

    /**
     * Copies the specified file to the specified directory
     * @param sourceFile  The full path of the file to be copied
     * @param targetFile    Full path to the target directory
     * @return  true:操作成功
     */
    public static boolean copyFile(@NonNull File sourceFile,@NonNull File targetFile){
        if(!sourceFile.exists())
            return false;
        try {
            FileInputStream ins = new FileInputStream(sourceFile);
            FileOutputStream out = new FileOutputStream(targetFile);
            byte[] b = new byte[1024];
            int n = 0;
            while ((n = ins.read(b)) != -1) {
                out.write(b, 0, n);
            }

            ins.close();
            out.close();
            Log.i("mat","copy file" + sourceFile.getAbsolutePath()+ "---------to--------" + targetFile.getAbsolutePath() + "  success");
        }catch (Exception e){
            Log.i("mat",e.getMessage());
            return false;
        }
        return true;
    }

    //压缩
    public static void zip(String folder,String dest) throws IOException{
        ZipUtils.zip(folder,dest);
    }

    //解压
    public static void unZip(String archive, String decompressDir) throws IOException{
        ZipUtils.unZip(archive,decompressDir);
    }

}
