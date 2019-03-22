package com.wxtoplink.base.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

/**
 * Created by 12852 on 2019/3/18.
 */

public class NetworkTools {

    //判断网络是否连接
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public static boolean isNetworkConnect(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isAvailable();
        }
        return false;
    }

    //判断wifi网络是否连接
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public static boolean isWifiConnect(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }
        return false;
    }

    //判断移动数据网络是否连接
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public static boolean isMobileConnect(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }
        return false;
    }

    /**
     * 获取wifi的信号强度
     * @param context
     * @return 信号强度（0->100）
     */
    @RequiresPermission(allOf = {android.Manifest.permission.ACCESS_WIFI_STATE,android.Manifest.permission.ACCESS_NETWORK_STATE})
    public static int getWifiRssi(@NonNull Context context) {
        if (isWifiConnect(context)) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            //该方法返回信号范围为-100 至0 ，0为信号最强
            return wifiInfo.getRssi();
        }
        return 0;
    }

    //获取wifi信号级别
    @RequiresPermission(allOf = {android.Manifest.permission.ACCESS_WIFI_STATE,android.Manifest.permission.ACCESS_NETWORK_STATE})
    public static StrengthLevel getWifiSignLevel(@NonNull Context context) {

        int rssi = getWifiRssi(context);
        switch (rssi / 20) {
            case 0:
                return StrengthLevel.WIFI_4;
            case -1:
                return StrengthLevel.WIFI_3;
            case -2:
                return StrengthLevel.WIFI_2;
            case -3:
                return StrengthLevel.WIFI_1;
            default:
                return StrengthLevel.WIFI_0;

        }
    }

    /**
     * 获取当前连接类型
     * @param context
     * @return
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public static NetworkInfo getConnectNetInfo(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable()) {
            return networkInfo;
        }
        return null;
    }

    //注册移动网络信号监听器
    public static void registerMobileStrengthsListener(@NonNull Context context, PhoneStateListener phoneStateListener) {
        mobileStrengthsListener(context, phoneStateListener, false);
    }

    //取消注册移动网络信号监听器
    public static void unregisterMobileStrengthsListener(@NonNull Context context, PhoneStateListener phoneStateListener) {
        mobileStrengthsListener(context, phoneStateListener, true);
    }

    //设置移动信号强度监听器
    public static void mobileStrengthsListener(@NonNull Context context, PhoneStateListener phoneStateListener, boolean cancel) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if(cancel){
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }else {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }
    }

    //获取移动信号强度dbm
    public static int getMobileDbm(SignalStrength signalStrength){
        int dbm = -120 ;
        if(signalStrength.isGsm()){
            //中国移动，没有直接的方法可以获取，这里使用反射获取，5.1版本及6.0版本经过测试
            try {
                Method method = SignalStrength.class.getMethod("getDbm");
                dbm = (int) method.invoke(signalStrength);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            //中国联通
            int cdmaDbm = signalStrength.getCdmaDbm() ;
            //中国电信
            int evdoDbm = signalStrength.getEvdoDbm() ;
            //-120表示无信号
            dbm = (evdoDbm == -120) ? cdmaDbm : ((cdmaDbm == -120) ? evdoDbm
                    : (cdmaDbm < evdoDbm ? cdmaDbm : evdoDbm));
        }

        return dbm ;
    }

    //获取移动网络信号级别
    public static StrengthLevel getMobileSignLevel(SignalStrength signalStrength){

        int signalLevel = 0 ;

        //6.0及以上提供直接获取信号强度的方法(0->4)由弱到强
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            signalLevel = signalStrength.getLevel();
        }else{
            //获取dbm，根据dbm判断信号强度
            int dbm = getMobileDbm(signalStrength);
            if(dbm < -105){
                signalLevel = 0 ;
            }else if(dbm < - 100){
                signalLevel = 1 ;
            }else if(dbm < -95){
                signalLevel = 2 ;
            }else if(dbm < -85){
                signalLevel = 3 ;
            }else{
                signalLevel = 4 ;
            }
        }
        switch (signalLevel){
            case 1:
                return StrengthLevel.MOBILE_1 ;
            case 2:
                return StrengthLevel.MOBILE_2 ;
            case 3:
                return StrengthLevel.MOBILE_3 ;
            case 4:
                return StrengthLevel.MOBILE_4 ;
            default:return StrengthLevel.MOBILE_0 ;
        }
    }

    //获取运营商名称
    public static String getSimOperatorName(Context context){
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if(telephonyManager != null && telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY)
            return telephonyManager.getSimOperatorName() ;
        else{
            return "SIM card not inserted";
        }
    }

    //获取网络类型
    @RequiresPermission(allOf = {android.Manifest.permission.ACCESS_WIFI_STATE,android.Manifest.permission.ACCESS_NETWORK_STATE})
    public static NetworkType getNetworkType(Context context){

        if(isWifiConnect(context)){//连接了wifi,则返回wifi类型
            return NetworkType.WIFI ;
        }else if(isNetworkConnect(context)) {

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            switch (telephonyManager.getNetworkType()) {
                case TelephonyManager.NETWORK_TYPE_GPRS://移动2G
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return NetworkType.MOBILE_2G;

                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return NetworkType.MOBILE_3G;

                case TelephonyManager.NETWORK_TYPE_LTE:
                    return NetworkType.MOBILE_4G;

                default:
                    return NetworkType.UNKNOWN;
            }
        }else{
            return NetworkType.UNKNOWN ;
        }
    }


}
