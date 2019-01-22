package com.wxtoplink.base.camera.utils;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

/**
 * Created by 12852 on 2019/1/21.
 */

public class USBCameraTool {

    private UsbManager usbManager ;
    private UsbDevice usbDevice ;

    public USBCameraTool(Context context) {
        init(context);
    }

    private void init(Context context){
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    public UsbManager getUsbManager(){
        return usbManager ;
    }

    public Iterable<UsbDevice> getUsbDevices(){
        return usbManager.getDeviceList().values();
    }


}
