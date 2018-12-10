package com.wxtoplink.base.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA1加密
 * Created by 12852 on 2018/10/29.
 */

public class EncryptionCheckUtil {

    //SHA1加密
    public static String encryptToSHA(String info) {
        byte[] digesta = null;
        try {
            MessageDigest alga = MessageDigest.getInstance("SHA-1");
            alga.update(info.getBytes());
            digesta = alga.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return byteToHexString(digesta);
    }


    //计算文件的md5
    public static String md5sum( String filePath) {
        if(!new File(filePath).exists()){
            return null;
        }
        InputStream fis;
        byte[] buffer = new byte[1024];
        int numRead = 0;
        MessageDigest md5;
        try{
            fis = new FileInputStream(filePath);
            md5 = MessageDigest.getInstance("MD5");
            while((numRead=fis.read(buffer)) > 0) {
                md5.update(buffer,0,numRead);
            }
            fis.close();
            return byteToHexString(md5.digest());
        } catch (Exception e) {
            System.out.println("error");
            return null;
        }
    }

    public static String byteToHexString(byte[] b) {
        final char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F' };
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[b[i] & 0x0f]);
        }
        return sb.toString();
    }

}
