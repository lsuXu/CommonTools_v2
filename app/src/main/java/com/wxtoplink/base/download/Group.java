package com.wxtoplink.base.download;

import com.wxtoplink.base.download.adapt.GroupListenerAdapt;

import java.util.ArrayList;
import java.util.List;

/**
 * 下载任务组
 * Created by 12852 on 2019/4/25.
 */

public class Group {

    private int groupId ;//组ID，唯一

    private List<DownloadTask> waitTasks , finishTasks ;//等待队列，完成队列

    private GroupListener groupListener ;//组监听器

    private Observer groupObserver ;//单独下载任务监听器，由下载任务调用通知组

    Group(int groupId, List<DownloadTask> waitTasks,GroupListener groupListener) {
        this.groupId = groupId;
        this.waitTasks = waitTasks;
        this.groupListener = groupListener ;
        this.finishTasks = new ArrayList<>();
        this.groupObserver = new Observer() {
            //下载完成触发
            @Override
            public void downloadFinish(DownloadService downloadService) {
                //从等待下载队列移除
                Group.this.waitTasks.remove(downloadService.getDownloadTask());
                //添加到下载完成队列
                Group.this.finishTasks.add(downloadService.getDownloadTask());
                //等待队列大小发生变化，触发组回调
                getGroupListener().waitSize(Group.this.waitTasks.size());
                if (Group.this.waitTasks.size() <= 0 ) {
                    //下载组任务全部完成，触发下载完成回调
                    getGroupListener().onFinish();
                }
            }
        };
    }

    //获取唯一ID
    public int getGroupId() {
        return groupId;
    }

    //获取等待队列
    public List<DownloadTask> getWaitTasks() {
        return waitTasks;
    }

    //获取组监听器
    GroupListener getGroupListener() {
        if(groupListener == null){
            groupListener = new GroupListenerAdapt();
        }
        return groupListener;
    }

    //通知开始添加到下载队列
    void notifyStart(){
        groupListener.onStart(waitTasks.size() + finishTasks.size());
    }

    //获取下载完成队列
    public List<DownloadTask> getFinishTasks() {
        return finishTasks;
    }

    //添加下载任务到任务组
    public boolean addTask(DownloadTask downloadTask){
        if(downloadTask.getStatus() == Status.PREPARE.getId()){//准备下载中的任务，禁止重复添加
            return false ;
        }
        return waitTasks.add(downloadTask);
    }

    public Observer getGroupObserver() {
        return groupObserver;
    }

    public interface GroupListener{

        //开始添加到下载线层，返回总大小
        void onStart(int totalSize);

        //队列剩余大小
        void waitSize(int size);

        //队列下载完成
        void onFinish();
    }


}
