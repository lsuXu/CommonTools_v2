package com.wxtoplink.base.download.adapt;

import com.wxtoplink.base.download.GroupBuilder;

/**
 * 下载任务组监听适配器
 * Created by 12852 on 2019/4/25.
 */

public class GroupListenerAdapt implements GroupBuilder.GroupListener {

    @Override
    public void onStart(int totalSize) {

    }

    @Override
    public void waitTaskSize(int size) {

    }

    @Override
    public void onFinish() {

    }
}
