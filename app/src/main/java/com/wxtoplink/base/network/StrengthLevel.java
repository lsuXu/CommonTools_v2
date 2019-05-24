package com.wxtoplink.base.network;

/**
 * Created by 12852 on 2019/3/21.
 */

public enum StrengthLevel {

    WIFI_0(0,"wifi"),//wifi信号最差
    WIFI_1(1,"wifi"),
    WIFI_2(2,"wifi"),
    WIFI_3(3,"wifi"),
    WIFI_4(4,"wifi"),//wifi信号最强
    MOBILE_0(0,"mobile"),//信号最差或无信号
    MOBILE_1(1,"mobile"),
    MOBILE_2(2,"mobile"),
    MOBILE_3(3,"mobile"),
    MOBILE_4(4,"mobile");//信号最强

    private int level ;

    private String type ;

    StrengthLevel(int level, String type) {
        this.level = level;
        this.type = type;
    }

    public int getLevel() {
        return level;
    }

    public String getType() {
        return type;
    }
}
