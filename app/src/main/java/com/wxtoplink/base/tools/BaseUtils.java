package com.wxtoplink.base.tools;

import android.os.Environment;

/**
 * Created by 12852 on 2018/8/9.
 */

public class BaseUtils {

    /**
     * 判断sd卡是否存在
     *
     * @return
     */
    public static boolean sdCardExit() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else
            return false;
    }

}
