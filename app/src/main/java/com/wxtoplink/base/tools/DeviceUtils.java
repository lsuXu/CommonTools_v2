package com.wxtoplink.base.tools;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by 12852 on 2018/8/27.
 */

public class DeviceUtils {

    private static final String TAG = DeviceUtils.class.getSimpleName();

    //获取设备mac地址
    public static String getMac(Context context) {

        //检查网络权限以及Wifi状态权限
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context,Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {

            //Android 6.0 以下，直接使用wlan的mac地址，6.0以上，获取所有网络信息，从中获取mac地址（可能获取到的是有线网卡的mac地址）
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                return wifiManager.getConnectionInfo().getMacAddress();
            } else {
                try {
                    //name{eth:有线网卡 ; wlan:无线网卡}
                    List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                    for (NetworkInterface networkInterface : networkInterfaces) {
                        byte[] macByte = networkInterface.getHardwareAddress();
                        if(macByte != null) {
                            StringBuilder stringBuilder = new StringBuilder();
                            for(byte b : macByte){
                                stringBuilder.append(String.format("%02X:",b));
                            }
                            stringBuilder.deleteCharAt(stringBuilder.length()-1);
                            Log.i(TAG,"mac :" + stringBuilder.toString());
                            return stringBuilder.toString();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return null ;
    }


    /**
     *  获取app版本
     * @return
     */
    public static String getAppVersionName(Context context) {
        String version = null;
        try {
            PackageInfo packageInfo = context.getApplicationContext().getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

    //运行内存
    public static String getRamUsedPercent(Context context){

        ActivityManager activityManager = (ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);

        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();

        activityManager.getMemoryInfo(memoryInfo);

        return String.valueOf( 1 - (float) memoryInfo.availMem/memoryInfo.totalMem);

    }

    //获取物理内存所占百分比
    public static String getRomUsedPercent(){

        //获取文件空间系统上的总体物理信息
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());

        return String.valueOf(1 - ((float)statFs.getAvailableBytes() / statFs.getTotalBytes())) ;
    }

    /**
     * 获取当前CPU占比
     * 在实际测试中发现，有的手机会隐藏CPU状态，不会完全显示所有CPU信息，例如MX5，所有建议只做参考
     * @return
     */
    public static double getProcessCpuRate(){
        String path = "/proc/stat";// 系统CPU信息文件
        long totalJiffies[]=new long[2];
        long totalIdle[]=new long[2];
        int firstCPUNum=0;//设置这个参数，这要是防止两次读取文件获知的CPU数量不同，导致不能计算。这里统一以第一次的CPU数量为基准
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        Pattern pattern= Pattern.compile(" [0-9]+");
        for(int i=0;i<2;i++) {
            totalJiffies[i]=0;
            totalIdle[i]=0;
            try {
                fileReader = new FileReader(path);
                bufferedReader = new BufferedReader(fileReader, 8192);
                int currentCPUNum=0;
                String str;
                while ((str = bufferedReader.readLine()) != null&&(i==0||currentCPUNum<firstCPUNum)) {
                    if (str.toLowerCase().startsWith("cpu")) {
                        currentCPUNum++;
                        int index = 0;
                        Matcher matcher = pattern.matcher(str);
                        while (matcher.find()) {
                            try {
                                long tempJiffies = Long.parseLong(matcher.group(0).trim());
                                totalJiffies[i] += tempJiffies;
                                if (index == 3) {//空闲时间为该行第4条栏目
                                    totalIdle[i] += tempJiffies;
                                }
                                index++;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(i==0){
                        firstCPUNum=currentCPUNum;
                        try {//暂停50毫秒，等待系统更新信息。
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        double rate=-1;
        if (totalJiffies[0]>0&&totalJiffies[1]>0&&totalJiffies[0]!=totalJiffies[1]){
            rate=1.0*((totalJiffies[1]-totalIdle[1])-(totalJiffies[0]-totalIdle[0]))/(totalJiffies[1]-totalJiffies[0]);
        }
        return rate;
    }

}
