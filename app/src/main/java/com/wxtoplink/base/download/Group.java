package com.wxtoplink.base.download;

import com.wxtoplink.base.download.adapt.GroupListenerAdapt;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 12852 on 2019/4/25.
 */

public class Group {

    private int groupId ;

    private List<DownloadTask> waitTasks , finishTasks ;

    private GroupListener groupListener ;

    private Observer groupObserver ;

    Group(int groupId, List<DownloadTask> waitTasks,GroupListener groupListener) {
        this.groupId = groupId;
        this.waitTasks = waitTasks;
        this.groupListener = groupListener ;
        this.groupObserver = new Observer() {
            @Override
            public void downloadFinish(DownloadService downloadService) {
                Group.this.waitTasks.remove(downloadService.getDownloadTask());
                Group.this.finishTasks.add(downloadService.getDownloadTask());
                getGroupListener().waitSize(Group.this.waitTasks.size());
                if (Group.this.waitTasks.size() <= 0 ) {
                    getGroupListener().onFinish();
                }
            }
        };
        this.finishTasks = new ArrayList<>();
    }

    public int getGroupId() {
        return groupId;
    }

    public List<DownloadTask> getWaitTasks() {
        return waitTasks;
    }

    public GroupListener getGroupListener() {
        if(groupListener == null){
            groupListener = new GroupListenerAdapt();
        }
        return groupListener;
    }

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

    public void setGroupListener(GroupListener groupListener) {
        this.groupListener = groupListener;
    }

    public Observer getGroupObserver() {
        return groupObserver;
    }

    public interface GroupListener{

        //队列剩余大小
        void waitSize(int size);

        void onFinish();
    }


}
