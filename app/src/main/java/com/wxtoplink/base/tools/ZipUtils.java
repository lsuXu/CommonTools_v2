package com.wxtoplink.base.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by 12852 on 2018/11/27.
 */

public class ZipUtils {

    /**
     * 解压文件
     * @param archive 需要解压的归档文件路径
     * @param decompressDir 解压后的目标文件夹路径
     * @throws IOException
     */
    public static void unZip(String archive, String decompressDir)throws IOException {
        BufferedInputStream bi;
        ZipFile zf = new ZipFile(archive);
        Enumeration e = zf.entries();
        while (e.hasMoreElements()) {
            ZipEntry ze2 = (ZipEntry) e.nextElement();
            String entryName = ze2.getName();
            String path = decompressDir + "/" + entryName;
            if (ze2.isDirectory())
            {
                File decompressDirFile = new File(path);
                if (!decompressDirFile.exists())
                {
                    decompressDirFile.mkdirs();
                }
            } else {
                String fileDir = path.substring(0, path.lastIndexOf("/"));
                File fileDirFile = new File(fileDir);
                if (!fileDirFile.exists())
                {
                    fileDirFile.mkdirs();
                }
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(decompressDir + "/" + entryName));
                bi = new BufferedInputStream(zf.getInputStream(ze2));
                byte[] readContent = new byte[1024];
                int readCount = bi.read(readContent);
                while (readCount != -1) {
                    bos.write(readContent, 0, readCount);
                    readCount = bi.read(readContent);
                }
                bos.close();
            }
        }
        zf.close();
    }

    /**
     * 压缩文件
     * @param src 需要压缩的（文件/文件夹）路径
     * @param dest 压缩完成后的归档文件
     * @throws IOException
     */
    public static void zip(String src, String dest) throws IOException{
        zip(new File(src),new File(dest));
    }

    /**
     * 压缩文件
     * @param srcFile 需要压缩的文件/文件夹
     * @param destFile 压缩完成后的归档文件
     * @throws IOException
     */
    public static void zip(File srcFile, File destFile) throws IOException {
        //提供了一个数据项压缩成一个ZIP归档输出流
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(destFile));
            //如果此文件是一个文件，否则为false。
            if (srcFile.isFile()) {
                zipFileOrDirectory(out, srcFile, "");
            } else {
                //返回一个文件或空阵列。
                File[] entries = srcFile.listFiles();
                for (int i = 0; i < entries.length; i++) {
                    // 递归压缩，更新curPaths
                    zipFileOrDirectory(out, entries[i], "");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //关闭输出流
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static void zipFileOrDirectory(ZipOutputStream out,
                                           File fileOrDirectory, String curPath) throws IOException {
        //从文件中读取字节的输入流
        FileInputStream in = null;
        try {
            //如果此文件是一个目录，否则返回false。
            if (!fileOrDirectory.isDirectory()) {
                // 压缩文件
                byte[] buffer = new byte[4096];
                int bytes_read;
                in = new FileInputStream(fileOrDirectory);
                //实例代表一个条目内的ZIP归档
                ZipEntry entry = new ZipEntry(curPath
                        + fileOrDirectory.getName());
                //条目的信息写入底层流
                out.putNextEntry(entry);
                while ((bytes_read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytes_read);
                }
                out.closeEntry();
            } else {
                // 压缩目录
                File[] entries = fileOrDirectory.listFiles();
                for (int i = 0; i < entries.length; i++) {
                    // 递归压缩，更新curPaths
                    zipFileOrDirectory(out, entries[i], curPath
                            + fileOrDirectory.getName() + "/");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}
